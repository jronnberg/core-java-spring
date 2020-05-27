package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionUpdateListener;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.Connection;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.ProviderSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryList;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryListDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreRuleBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreRuleDto;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystem;
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
    private final RuleMap ruleMap;
    private final SystemTracker systemTracker;

    /**
     * Class constructor.
     *
     * @param client Object for sending HTTP messages to the Orchestrator.
     * @param cloud DTO describing a Arrowhead Cloud.
     * @param SystemTracker Object used to keep track of registered Arrowhead
     *                      systems.
     */
    public OrchestratorClient(HttpClient client, CloudDto cloud, SystemTracker systemTracker) {
        Objects.requireNonNull(client, "Expected HttpClient");
        Objects.requireNonNull(cloud, "Expected cloud");
        Objects.requireNonNull(systemTracker, "Expected System tracker");

        this.client = client;
        this.cloud = cloud;
        this.systemTracker = systemTracker;
        ruleMap = new RuleMap();

        SrSystem orchestrator = systemTracker.getSystem(ORCHESTRATOR_SYSTEM_NAME);
        Objects.requireNonNull(orchestrator, "Expected Orchestrator system to be available via Service Registry.");

        this.orchestratorAddress = new InetSocketAddress(orchestrator.address(), orchestrator.port());
    }

    /**
     * Retrieve all active rules from the Orchestrator.
     *
     * @return A Future which will contain the requested rules.
     */
    private Future<StoreEntryListDto> getRules() {
        return client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/orchestrator/mgmt/store")
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, StoreEntryListDto.class));
    }

    /**
     * Initializes the Orchestrator client.
     *
     * @param entryMap
     * @return
     */
    public Future<Void> initialize(PlantDescriptionEntryMap entryMap) {

        entryMap.addListener(this);
        activeEntry = entryMap.activeEntry();

        // TODO: This is a temporary solution.
        // Delete all rules in the orchestrator, create new ones for the active
        // Plant Description Entry.
        return getRules()
            .flatMap(rules -> {
                var deletions = rules.data()
                    .stream()
                    .map(rule -> deleteRule(rule.id()))
                    .collect(Collectors.toList());

                return Futures.serialize(deletions).flatMap(deletionResult -> {
                    if (logger.isInfoEnabled()) {
                        logger.info("Deleted all rules in Orchestrator.");
                    }

                    if (activeEntry == null) {
                        return Future.done();
                    }

                    return postRules(activeEntry)
                        .flatMap(postResult -> {
                            if (logger.isInfoEnabled()) {
                                logger.info("Created rules for Plant Description Entry '"
                                    + activeEntry.plantDescription() + "'.");
                            }
                            return Future.done();
                        });
                });
            });

    }

    /**
     * Create an Orchestrator rule to be passed to the Orchestrator.
     *
     * @param entry           Plant Description Entry to which the rule will belong.
     * @param connectionIndex The index of the related connection within the entry's
     *                        connection list.
     * @return An Orchestrator rule that embodies the specified connection.
     */
    private StoreRuleDto createRule(PlantDescriptionEntry entry, int connectionIndex) {
        Objects.requireNonNull(entry, "Expected Plant Description Entry");

        final Connection connection = entry.connections().get(connectionIndex);

        SrSystem consumerSystem = systemTracker.getSystem(connection.consumer().systemId());
        SrSystem providerSystem = systemTracker.getSystem(connection.producer().systemId());

        Objects.requireNonNull(consumerSystem, "Consumer system '" + connection.consumer().systemId() + "' not found in Service Registry"); // TODO: Proper handling, raise an alarm?
        Objects.requireNonNull(providerSystem, "Producer system '" + connection.producer().systemId() + "' not found in Service Registry"); // TODO: Proper handling, raise an alarm?

        var builder = new StoreRuleBuilder()
            .cloud(cloud)
            .serviceDefinitionName(entry.serviceDefinitionName(connectionIndex))
            .consumerSystemId(consumerSystem.id())
            .providerSystem(new ProviderSystemBuilder()
                .systemName(providerSystem.systemName())
                .address(providerSystem.address())
                .port(providerSystem.port())
                .build())
            .priority(1); // TODO: Remove hard-coded value

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
     * For each connection in the given entry, a corresponding rule is posted to
     * the Orchestrator.
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

        return systemTracker.refreshSystems() // TODO: Make the system tracker refresh itself automatically instead.
            .flatMap(result -> {
                for (int i = 0; i < numConnections; i++) {
                    rules.add(createRule(entry, i));
                }

                return client
                    .send(orchestratorAddress, new HttpClientRequest()
                        .method(HttpMethod.POST)
                        .uri("/orchestrator/mgmt/store")
                        .body(DtoEncoding.JSON, rules)
                        .header("accept", "application/json"))
                    .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, StoreEntryListDto.class));
            });
    }

    /**
     * @return An empty {@code StoreEntryListDto}.
     */
    private StoreEntryListDto emptyRuleList() {
        return new StoreEntryListBuilder().count(0).data(new ArrayList<>()).build();
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
     * Deletes all orchestrator rules for a given entry, if any. If the entry is
     * null, a completed Future is immediately returned.
     *
     * @param entry The entry whose rules are to be deleted.
     * @return A Future that performs the deletions.
     */
    private Future<Void> deleteRules(PlantDescriptionEntry entry) {
        if (entry == null) {
            return Future.done();
        }

        List<Integer> rulesToRemove = ruleMap.get(entry.id());

        if (rulesToRemove == null) {
            return Future.done();
        }

        var deletions = rulesToRemove.stream().map(ruleId -> deleteRule(ruleId)).collect(Collectors.toList());

        return Futures.serialize(deletions).flatMap(result -> {
            if (logger.isInfoEnabled()) {
                logger.info("Orchestrator rules belonging to Plant Description Entry '" + entry.plantDescription()
                        + "' deleted.");
            }
            ruleMap.remove(entry.id());
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
                // TODO: This isn't strictly true: the count is zero if the rules already exist.
                logger.warn("The active Plant Description (" + entryName + ") does not have any connections.");
            }
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

        final Future<Void> deleteRulesTask = shouldDeleteCurrentRules ? deleteRules(activeEntry) : Future.done();
        final Future<StoreEntryListDto> postRulesTask = shouldPostRules
            ? postRules(entry)
            : Future.success(emptyRuleList());

        deleteRulesTask
            .flatMap(result -> postRulesTask)
            .ifSuccess(ruleList -> {
                if (entry.active()) {
                    activeEntry = entry;
                    ruleMap.put(entry.id(), ruleList);
                    logEntryActivated(entry, ruleList);
                } else if (entryWasDeactivated) {
                    activeEntry = null;
                    if (logger.isInfoEnabled()) {
                        logger.info("Deactivated Plant Description '" + entry.plantDescription() + "'");
                    }
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
        deleteRules(entry)
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