package eu.arrowhead.core.plantdescriptionengine.orchestratorclient;

import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.*;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystem;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.RuleStore;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.RuleStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionUpdateListener;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.Connection;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystem;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.dto.DtoWritable;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Futures;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class OrchestratorClient implements PlantDescriptionUpdateListener {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorClient.class);

    private final HttpClient client;
    private final InetSocketAddress orchestratorAddress;
    private final CloudDto cloud;
    private final RuleStore ruleStore;
    private final SystemTracker systemTracker;
    private final PlantDescriptionTracker pdTracker;
    private PlantDescriptionEntry activeEntry = null;

    /**
     * Class constructor.
     *
     * @param httpClient    Object for sending HTTP messages to the Orchestrator.
     * @param cloud         DTO describing a Arrowhead Cloud.
     * @param ruleStore     Object providing permanent storage for Orchestration
     *                      rule data.
     * @param systemTracker Object used to track registered Arrowhead systems.
     */
    public OrchestratorClient(HttpClient httpClient, CloudDto cloud, RuleStore ruleStore, SystemTracker systemTracker,
                              PlantDescriptionTracker pdTracker) {

        Objects.requireNonNull(httpClient, "Expected HttpClient");
        Objects.requireNonNull(cloud, "Expected cloud");
        Objects.requireNonNull(ruleStore, "Expected backing store");
        Objects.requireNonNull(systemTracker, "Expected System Tracker");
        Objects.requireNonNull(pdTracker, "Expected Plant Description Tracker");

        this.client = httpClient;
        this.cloud = cloud;
        this.systemTracker = systemTracker;
        this.ruleStore = ruleStore;
        this.pdTracker = pdTracker;

        String ORCHESTRATOR_SYSTEM_NAME = "orchestrator";
        SrSystem orchestrator = systemTracker.getSystemByName(ORCHESTRATOR_SYSTEM_NAME);
        Objects.requireNonNull(orchestrator, "Expected Orchestrator system to be available via Service Registry.");

        this.orchestratorAddress = new InetSocketAddress(orchestrator.address(), orchestrator.port());
    }

    /**
     * Initializes the Orchestrator client.
     *
     * @return A {@code Future} which will complete once Orchestrator rules have
     * been created for the connections in the active Plant Description
     * entry (if any).
     */
    public Future<Void> initialize() {
        pdTracker.addListener(this);
        activeEntry = pdTracker.activeEntry();

        return deleteActiveRules()
            .flatMap(deletionResult -> (activeEntry == null) ? Future.done() : postRules().flatMap(createdRules -> {
                ruleStore.setRules(createdRules.getIds());
                logger.info("Created rules for Plant Description Entry '" + activeEntry.plantDescription() + "'.");
                return Future.done();
            }));
    }

    /**
     * Create an Orchestrator rule to be passed to the Orchestrator.
     *
     * @param connection A connection between a producer and consumer system present
     *                   in a Plant Description Entry.
     * @return An Orchestrator rule that embodies the specified connection.
     */
    StoreRuleDto createRule(Connection connection) {

        Objects.requireNonNull(connection, "Expected a connection");

        final String consumerId = connection.consumer().systemId();
        final String providerId = connection.producer().systemId();

        final PdeSystem consumer = pdTracker.getSystem(consumerId);
        final PdeSystem provider = pdTracker.getSystem(providerId);

        // TODO: In the future, we will be able to create Orchestration rules
        // using system name *or* metadata. For now, we assume that systemName
        // is present.

        if (consumer.systemName().isEmpty() || provider.systemName().isEmpty()) {
            logger.error(
                "Failed to create Orchestrator rule. The current version of the PDE requires all Plant Description systems to specify a system name.");
            return null;
        }

        final SrSystem consumerSystemSrEntry = systemTracker.getSystemByName(consumer.systemName().orElse(null));
        final SrSystem providerSystemSrEntry = systemTracker.getSystemByName(provider.systemName().orElse(null));

        if (consumerSystemSrEntry == null || providerSystemSrEntry == null) {
            return null;
        }

        String portName = connection.producer().portName();
        String serviceDefinition = pdTracker.getServiceDefinition(portName);

        var builder = new StoreRuleBuilder().cloud(cloud).serviceDefinitionName(serviceDefinition)
            .consumerSystemId(consumerSystemSrEntry.id()).attribute(provider.portMetadata(portName))
            .providerSystem(new ProviderSystemBuilder().systemName(providerSystemSrEntry.systemName())
                .address(providerSystemSrEntry.address()).port(providerSystemSrEntry.port()).build())
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
     * <p>
     * For each connection in the given entry, a corresponding rule is posted to the
     * Orchestrator.
     *
     * @return A Future which will contain a list of the created rules.
     */
    private Future<StoreEntryListDto> postRules() {
        var connections = pdTracker.getActiveConnections();

        if (connections.isEmpty()) {
            // Return immediately with an empty rule list:
            return Future.success(emptyRuleList());
        }

        List<DtoWritable> rules = new ArrayList<>();

        for (var connection : connections) {
            var rule = createRule(connection);
            if (rule != null) {
                rules.add(rule);
            }
        }

        if (rules.size() == 0) {
            return Future.success(emptyRuleList());
        }

        return client
            .send(orchestratorAddress,
                new HttpClientRequest().method(HttpMethod.POST).uri("/orchestrator/mgmt/store")
                    .body(DtoEncoding.JSON, rules).header("accept", "application/json"))
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
        return client
            .send(orchestratorAddress,
                new HttpClientRequest().method(HttpMethod.DELETE).uri("/orchestrator/mgmt/store/" + id))
            .flatMap(response -> {
                if (response.status() != HttpStatus.OK) {
                    // TODO: Throw some other type of Exception.
                    return Future.failure(new RuntimeException("Failed to delete store rule with ID " + id));
                }
                return Future.done();
            });
    }

    /**
     * Deletes all orchestrator rules created by the Orchestrator client.
     *
     * @return A Future that performs the deletions.
     */
    private Future<Void> deleteActiveRules() {

        Set<Integer> rules;
        try {
            rules = ruleStore.readRules();
        } catch (RuleStoreException e) {
            return Future.failure(e);
        }

        if (rules.isEmpty()) {
            return Future.done();
        }

        // Delete any rules previously created by the Orchestrator client:
        var deletions = new ArrayList<Future<Void>>();

        for (int rule : rules) {
            deletions.add(deleteRule(rule));
        }

        return Futures.serialize(deletions).flatMap(result -> {
            ruleStore.removeAll();
            logger.info("Deleted all orchestrator rules created by the Orchestrator client.");
            return Future.done();
        });
    }

    /**
     * Logs the fact that the specified entry has been activated.
     *
     * @param entry    A Plant Description Entry.
     * @param ruleList The list of Orchestrator rules connected to the entry.
     */
    private void logEntryActivated(PlantDescriptionEntry entry, StoreEntryList ruleList) {
        Objects.requireNonNull(entry, "Expected Plant Description Entry");

        String entryName = entry.plantDescription();

        if (ruleList.count() > 0) {
            String msg = "Orchestrator rules created for Plant Description '" + entryName + "': [";
            List<String> ids = new ArrayList<>();
            for (var rule : ruleList.data()) {
                ids.add(rule.id().toString());
            }
            msg += String.join(", ", ids) + "]";
            logger.info(msg);
        } else {
            logger.warn("No new rules were created for Plant Description '" + entryName + "'."); // TODO: Should
            // something be done in
            // this case?
        }
    }

    /**
     * Handles an update of a Plant Description Entry.
     * <p>
     * Deletes and/or creates rules in the Orchestrator as appropriate.
     *
     * @param entry The updated entry.
     */
    @Override
    public void onPlantDescriptionUpdated(PlantDescriptionEntry entry) {

        int numConnections = pdTracker.getActiveConnections().size();
        boolean wasDeactivated = !entry.active() && activeEntry != null && activeEntry.id() == entry.id();
        boolean shouldPostRules = entry.active() && numConnections > 0;
        boolean shouldDeleteCurrentRules = entry.active() || wasDeactivated;

        final Future<Void> deleteRulesTask = shouldDeleteCurrentRules ? deleteActiveRules() : Future.done();

        final Future<StoreEntryListDto> postRulesTask = shouldPostRules ? postRules() : Future.success(emptyRuleList());

        deleteRulesTask.flatMap(result -> postRulesTask).ifSuccess(createdRules -> {
            if (entry.active()) {
                activeEntry = entry;
                ruleStore.setRules(createdRules.getIds());
                logEntryActivated(entry, createdRules);

            } else if (wasDeactivated) {
                activeEntry = null;
                logger.info("Deactivated Plant Description '" + entry.plantDescription() + "'");
            }
        }).onFailure(throwable -> logger.error(
            "Encountered an error while handling the new Plant Description '" + entry.plantDescription() + "'",
            throwable));
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
            .ifSuccess(result -> logger.info("Deleted all Orchestrator rules belonging to Plant Description Entry '"
                + entry.plantDescription() + "'"))
            .onFailure(throwable -> logger.error(
                "Encountered an error while attempting to delete Plant Description '" + entry.plantDescription() + "'",
                throwable));
    }

}