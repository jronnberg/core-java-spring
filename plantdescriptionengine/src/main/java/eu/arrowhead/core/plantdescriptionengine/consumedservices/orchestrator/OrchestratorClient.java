package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionUpdateListener;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.Connection;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystem;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.CloudDto;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.ProviderSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntry;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntryList;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntryListDto;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreRuleBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreRuleDto;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.RuleStore;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.RuleStoreException;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystem;
import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.dto.DtoWritable;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Futures;

public class OrchestratorClient implements PlantDescriptionUpdateListener {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorClient.class);

    private final HttpClient client;
    private final InetSocketAddress orchestratorAddress;
    private final CloudDto cloud;
    private final String ORCHESTRATOR_SYSTEM_NAME = "orchestrator";
    private PlantDescriptionEntry activeEntry = null;
    private final Set<Integer> activeRules = new HashSet<>(); // TODO: Concurrency handling?
    private final RuleStore backingStore;


    /**
     * Class constructor.
     *
     * @param httpClient Object for sending HTTP messages to the Orchestrator.
     * @param cloud      DTO describing a Arrowhead Cloud.
     * @param ruleStore  Object providing permanent storage for Orchestration
     *                   rule data.
     * @throws RuleStoreException
     */
    public OrchestratorClient(HttpClient httpClient, CloudDto cloud, RuleStore ruleStore) throws RuleStoreException {
        Objects.requireNonNull(httpClient, "Expected HttpClient");
        Objects.requireNonNull(cloud, "Expected cloud");
        Objects.requireNonNull(ruleStore, "Expected backing store");

        this.client = httpClient;
        this.cloud = cloud;
        this.backingStore = ruleStore;

        SrSystem orchestrator = Locator.getSystemTracker().getSystemByName(ORCHESTRATOR_SYSTEM_NAME);
        Objects.requireNonNull(orchestrator, "Expected Orchestrator system to be available via Service Registry.");

        this.orchestratorAddress = new InetSocketAddress(orchestrator.address(), orchestrator.port());
    }

    /**
     * Initializes the Orchestrator client.
     *
     * @param pdTracker A Plant Description tracker.
     * @return
     */
    public Future<Void> initialize(PlantDescriptionTracker pdTracker) {

        pdTracker.addListener(this);
        activeEntry = pdTracker.activeEntry();

        return deleteActiveRules().flatMap(deletionResult -> {

            if (activeEntry == null) {
                return Future.done();
            }

            return postRules(activeEntry).flatMap(postResult -> {
                if (logger.isInfoEnabled()) {
                    logger.info("Created rules for Plant Description Entry '" + activeEntry.plantDescription() + "'.");
                }
                return Future.done();
            });
        });
    }

    /**
     * Create an Orchestrator rule to be passed to the Orchestrator.
     *
     * @param entry           Plant Description Entry to which the rule will belong.
     * @param connectionIndex The index of the related connection within the entry's
     *                        connection list.
     *
     * @return An Orchestrator rule that embodies the specified connection.
     */
    StoreRuleDto createRule(PlantDescriptionEntry entry, int connectionIndex) {
        Objects.requireNonNull(entry, "Expected Plant Description Entry");

        final Connection connection = entry.connections().get(connectionIndex);
        final String consumerId = connection.consumer().systemId();
        final String providerId = connection.producer().systemId();
        final PdeSystem consumer = entry.getSystem(consumerId);
        final PdeSystem provider = entry.getSystem(providerId);

        // TODO: In the future, we will be able to create Orchestration rules
        // using system name *or* metadata. For now, we assume that systemName
        // is present.

        if (!consumer.systemName().isPresent() || !provider.systemName().isPresent()) {
            logger.error("Cannot create rules for Plant Description '" +
                entry.plantDescription() +
                "'. The current version of the PDE requires all Plant Description systems to specify a system name.");
            return null;
        }

        final SystemTracker systemTracker = Locator.getSystemTracker();
        final SrSystem consumerSystemSrEntry = systemTracker.getSystemByName(consumer.systemName().get());
        final SrSystem providerSystemSrEntry = systemTracker.getSystemByName(provider.systemName().get());

        if (consumerSystemSrEntry == null || providerSystemSrEntry == null) {
            return null;
        }

        String portName = connection.producer().portName();

        var builder = new StoreRuleBuilder()
            .cloud(cloud)
            .serviceDefinitionName(entry.serviceDefinitionName(connectionIndex))
            .consumerSystemId(consumerSystemSrEntry.id())
            .attribute(provider.portMetadata(portName))
            .providerSystem(new ProviderSystemBuilder()
                .systemName(providerSystemSrEntry.systemName())
                .address(providerSystemSrEntry.address())
                .port(providerSystemSrEntry.port())
                .build())
            .priority(1); // What priority should be used?

        if (client.isSecure()) {
            builder.serviceInterfaceName("HTTP-SECURE-JSON");
        } else {
            builder.serviceInterfaceName("HTTP-INSECURE-JSON");
        }

        return builder.build();
    }

    /**
     * Posts Orchestrator rules for the given Plant Description Entry.
     *
     * For each connection in the given entry, a corresponding rule is posted to the
     * Orchestrator.
     *
     * @param entry A Plant Description Entry.
     * @return A Future which will contain a list of the created rules.
     */
    private Future<StoreEntryListDto> postRules(PlantDescriptionEntry entry) {
        int numConnections = entry.connections().size();

        if (numConnections == 0) {
            // Return immediately with an empty rule list:
            return Future.success(emptyRuleList());
        }

        List<DtoWritable> rules = new ArrayList<>();

        for (int i = 0; i < numConnections; i++) {
            var rule = createRule(entry, i);
            if (rule != null) { // TODO: Remove this check when createRule() has been fixed (no longer returns null)
                rules.add(rule);
            }
        }

        if (rules.size() == 0) {
            return Future.success(emptyRuleList());
        }

        return client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.POST)
            .uri("/orchestrator/mgmt/store")
            .body(DtoEncoding.JSON, rules)
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, StoreEntryListDto.class));
    }

    /**
     * @return An empty {@code StoreEntryListDto}.
     */
    private StoreEntryListDto emptyRuleList() {
        return new StoreEntryListBuilder().count(0).build();
    }

    /**
     * Deletes a single Orchestrator Store Entry.
     *
     * @param id The ID of an Orchestrator Store Entry to delete.
     * @return A Future that performs the deletion.
     */
    private Future<Void> deleteRule(int id) {
        return client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.DELETE)
            .uri("/orchestrator/mgmt/store/" + id))
        .flatMap(response -> {
            return Future.done();
        });
    }

    /**
     * Deletes all orchestrator rules created by the Orchestrator client.
     *
     * @param entry The entry whose rules are to be deleted.
     * @return A Future that performs the deletions.
     */
    private Future<Void> deleteActiveRules() {

        if (activeRules.isEmpty()) {
            return Future.done();
        }

        // Delete any rules previously created by the Orchestrator client:
        var deletions = new ArrayList<Future<Void>>();
        for (var ruleId : activeRules) {
            deletions.add(deleteRule(ruleId));
        }

        return Futures.serialize(deletions).flatMap(result -> {
            activeRules.clear();
            backingStore.removeAll();
            if (logger.isInfoEnabled()) {
                logger.info("Deleted all orchestrator rules created by the Orchestrator client.");
            }
            return Future.done();
        });
    }

    /**
     * Logs the fact that the specified entry has been activated.
     *
     * @param entry A Plant Description Entry.
     * @param ruleList The list of Orchestrator rules connected to the entry.
     */
    private void logEntryActivated(PlantDescriptionEntry entry, StoreEntryList ruleList) {
        Objects.requireNonNull(entry, "Expected Plant Description Entry");

        String entryName = entry.plantDescription();

        if (ruleList.count() > 0) {
            if (logger.isInfoEnabled()) {
                String msg = "Orchestrator rules created for Plant Description '" + entryName + "': [";
                List<String> ids = new ArrayList<>();
                for (var rule : ruleList.data()) {
                    ids.add(rule.id().toString());
                }
                msg += String.join(", ", ids) + "]";
                logger.info(msg);
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("No new rules were created for Plant Description '" + entryName + "'."); // TODO: Should something be done in this case?
            }
        }
    }

    /**
     * Logs the fact that the specified entry has been deactivated.
     *
     * @param entry The deactivated Plant Description Entry.
     */
    private void logEntryDeactivated(PlantDescriptionEntry entry) {
        if (logger.isInfoEnabled()) {
            logger.info("Deactivated Plant Description '" + entry.plantDescription() + "'");
        }
    }

    /**
     * Handles an update to a Plant Description Entry.
     *
     * Deletes and/or creates rules in the Orchestrator as appropriate.
     *
     * @param entry The updated entry.
     */
    @Override
    public void onPlantDescriptionUpdated(PlantDescriptionEntry entry) {

        boolean entryWasDeactivated = !entry.active() && activeEntry != null && activeEntry.id() == entry.id();
        boolean shouldPostRules = entry.active() && entry.connections().size() > 0;
        boolean shouldDeleteCurrentRules = entry.active() || entryWasDeactivated;

        final Future<Void> deleteRulesTask = shouldDeleteCurrentRules ? deleteActiveRules() : Future.done();

        final Future<StoreEntryListDto> postRulesTask = shouldPostRules
            ? postRules(entry)
            : Future.success(emptyRuleList());

        deleteRulesTask
            .flatMap(result -> postRulesTask)
            .ifSuccess(createdRules -> {
                if (entry.active()) {
                    activeEntry = entry;
                    final var ruleIds = createdRules.data().stream().map(StoreEntry::id).collect(Collectors.toSet());
                    backingStore.setRules(ruleIds);
                    activeRules.addAll(ruleIds);
                    logEntryActivated(entry, createdRules);

                } else if (entryWasDeactivated) {
                    activeEntry = null;
                    logEntryDeactivated(entry);
                }
            })
            .onFailure(throwable -> {
                logger.error("Encountered an error while handling the new Plant Description '"
                    + entry.plantDescription() + "'", throwable);

            });
    }

    /**
     * Handles the addition of a new Plant Description Entry.
     *
     * @param entry The added entry.
     */
    @Override
    public void onPlantDescriptionAdded(PlantDescriptionEntry entry) {
        onPlantDescriptionUpdated(entry);
    }

    /**
     * Handles the removal of a Plant Description Entry.
     *
     * @param entry The entry that has been removed.
     */
    @Override
    public void onPlantDescriptionRemoved(PlantDescriptionEntry entry) {

        // If the removed Plant Description was not active, there is no more
        // work to be done.
        if (activeEntry == null || activeEntry.id() != entry.id()) {
            return;
        }

        // Otherwise, all of its Orchestration rules should be deleted:
        deleteActiveRules()
            .ifSuccess(result -> {
                if (logger.isInfoEnabled()) {
                    logger.info("Deleted all Orchestrator rules belonging to Plant Description Entry '"
                        + entry.plantDescription() + "'");
                }
            })
            .onFailure(throwable -> {
                logger.error("Encountered an error while attempting to delete Plant Description '"
                    + entry.plantDescription() + "'", throwable);
            });
    }

}