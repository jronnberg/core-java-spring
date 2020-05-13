package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    final RuleMap rules = new RuleMap();

    /**
     * Class constructor.
     *
     * @param client Object for sending HTTP messages to the orchestrator.
     * @param cloud  DTO describing a Arrowhead Cloud.
     */
    public OrchestratorClient(HttpClient client, CloudDto cloud) {
        Objects.requireNonNull(client, "Expected HttpClient");
        Objects.requireNonNull(cloud, "Expected cloud");

        this.client = client;
        this.cloud = cloud;

        SrSystem orchestrator = SystemTracker.INSTANCE.getSystem(ORCHESTRATOR_SYSTEM_NAME);
        Objects.requireNonNull(orchestrator, "Expected Orchestrator system to be available via Service Registry.");

        this.orchestratorAddress = new InetSocketAddress(orchestrator.address(), orchestrator.port());
    }

    /**
     * Create an Orchestrator rule to be passed to the orchestrator.
     *
     * @param entry           Plant Description Entry to which the rule will belong.
     * @param connectionIndex The index of the related connection within the entry's
     *                        connection list.
     * @return An Orchestrator rule that embodies the specified connection.
     */
    private StoreRuleDto createRule(PlantDescriptionEntry entry, int connectionIndex) {
        Objects.requireNonNull(entry, "Expected Plant Description Entry");

        final Connection connection = entry.connections().get(connectionIndex);

        SrSystem consumerSystem = SystemTracker.INSTANCE.getSystem(connection.consumer().systemName());
        SrSystem providerSystem = SystemTracker.INSTANCE.getSystem(connection.producer().systemName());

        Objects.requireNonNull(consumerSystem, "Consumer system '" + connection.consumer().systemName() + "' not found in Service Registry"); // TODO: Proper handling, raise an alarm?
        Objects.requireNonNull(providerSystem, "Producer system '" + connection.producer().systemName() + "' not found in Service Registry"); // TODO: Proper handling, raise an alarm?

        return new StoreRuleBuilder()
            .cloud(cloud)
            .serviceDefinitionName(entry.serviceDefinitionName(connectionIndex))
            .consumerSystemId(consumerSystem.id())
            .providerSystem(new ProviderSystemBuilder()
                .systemName(providerSystem.systemName())
                .address(providerSystem.address())
                .port(providerSystem.port())
                .build())
            .priority(1) // TODO: Remove hard-coded value
            .serviceInterfaceName("HTTP-INSECURE-JSON") // TODO: Remove hard-coded value
            .build();
    }

    /**
     * Posts Orchestrator rules for the given Plant Description Entry.
     *
     * For each connection in the given entry, a corresponding rule is posted to the
     * orchestrator.
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
            rules.add(createRule(entry, i));
        }

        return client
            .send(orchestratorAddress, new HttpClientRequest()
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
        return new StoreEntryListBuilder().count(0).data(new ArrayList<>()).build();
    }

    /**
     * Deletes a single orchestrator rule (StoreEntry).
     *
     * @param id The ID of an orchestrator rule to delete.
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

        List<Integer> rulesToRemove = rules.get(entry.id());
        var deletions = rulesToRemove.stream().map(ruleId -> deleteRule(ruleId)).collect(Collectors.toList());

        return Futures.serialize(deletions).flatMap(result -> {
            if (logger.isInfoEnabled()) {
                logger.info("Orchestrator rules belonging to Plant Description Entry '" + entry.plantDescription()
                        + "' deleted.");
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
                logger.warn("The active Plant Description (" + entryName + ") does not have any connections.");
            }
        }
    }

    /**
     * Handles an update to a Plant Description Entry.
     *
     * @param entry The updated entry.
     */
    @Override
    public void onPlantDescriptionUpdated(PlantDescriptionEntry entry) {

        boolean entryWasDeactivated = !entry.active() && activeEntry != null && activeEntry.id() == entry.id();
        boolean shouldCreateRules = entry.active() && entry.connections().size() > 0;
        boolean shouldDeleteCurrentRules = entry.active() || entryWasDeactivated;

        final Future<Void> deleteRulesTask = shouldDeleteCurrentRules ? deleteRules(activeEntry) : Future.done();
        final Future<StoreEntryListDto> createRulesTask = shouldCreateRules
            ? postRules(entry)
            : Future.success(emptyRuleList());

        deleteRulesTask.
            flatMap(result -> createRulesTask)
            .ifSuccess(ruleList -> {
                if (entry.active()) {
                    activeEntry = entry;
                    rules.put(entry.id(), ruleList);
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