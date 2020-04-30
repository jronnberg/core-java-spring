package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import eu.arrowhead.core.plantdescriptionengine.services.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.services.management.PlantDescriptionUpdateListener;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.Connection;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.ProviderSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryListDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreRuleBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreRuleDto;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystem;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.dto.DtoWritable;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Futures;

public class OrchestratorClient implements PlantDescriptionUpdateListener {

    private final HttpClient client;
    private final InetSocketAddress orchestratorAddress;
    private final CloudDto cloud;

    public OrchestratorClient(HttpClient client, String orchestratorAddress, int orchestratorPort, CloudDto cloud) {
        Objects.requireNonNull(client, "Expected HttpClient");
        Objects.requireNonNull(cloud, "Expected cloud");
        Objects.requireNonNull(orchestratorAddress, "Expected orchestrator address");

        this.client = client;
        this.cloud = cloud;
        this.orchestratorAddress = new InetSocketAddress(orchestratorAddress, orchestratorPort);
    }

    private Future<StoreEntryListDto> getStoreEntries() {
        return client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/orchestrator/mgmt/store")
            .header("accept", "application/json")
        )
        .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, StoreEntryListDto.class));
    }

    private StoreRuleDto createRule(PlantDescriptionEntry entry, int connectionIndex) {

        final Connection connection = entry.connections().get(connectionIndex);

        SrSystem consumerSystem = SystemTracker.get(connection.consumer().systemName());
        SrSystem providerSystem = SystemTracker.get(connection.producer().systemName());

        return new StoreRuleBuilder()
            .serviceDefinitionName(entry.serviceDefinitionName(connectionIndex))
            .priority(1) // TODO: Remove hard-coded value
            .consumerSystemId(consumerSystem.id())
            .providerSystem(new ProviderSystemBuilder()
                .systemName(providerSystem.systemName())
                .address(providerSystem.address())
                .port(providerSystem.port())
                .build())
            .serviceInterfaceName("HTTP-INSECURE-JSON") // TODO: Remove hard-coded value
            .cloud(cloud)
            .build();
    }

    private Future<StoreEntryListDto> postRules(PlantDescriptionEntry entry) {

        int numConnections = entry.connections().size();
        List<DtoWritable> rules = new ArrayList<>();

        for (int i = 0; i < numConnections; i++) {
            rules.add(createRule(entry, i));
        }

        return client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.POST)
            .uri("/orchestrator/mgmt/store")
            .body(DtoEncoding.JSON, rules)
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, StoreEntryListDto.class))
            .map(storeEntryList -> {
                // rules.put(entry.id(), connectionIndex, storeEntry.id());
                return storeEntryList;
            });
    }

    private Future<Void> deleteRule(int id) {
        return client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.DELETE)
            .uri("/orchestrator/mgmt/store/" + id))
            .flatMap(response -> {
                return Future.done();
            });
    }

    // TODO: Remove this function.
    private Future<Void> removeAllRules() {
        return getStoreEntries()
            .flatMap(entryList -> {
                if (entryList.count() == 0) {
                    return Future.done();
                }

                var deletions = entryList.data()
                    .stream()
                    .map(entry -> deleteRule(entry.id()))
                    .collect(Collectors.toList());

                return Futures.serialize(deletions)
                    .flatMap(result -> {
                        System.out.println("All orchestrator rules deleted.");
                        return Future.done();
                    });
            });
    }

    @Override
    public void onUpdate(List<PlantDescriptionEntryDto> entries) {
        removeAllRules()
        .ifSuccess(removeResult -> {
            // TODO: Only post rules for the active entry
            var posts = entries.stream()
                .map(entry -> postRules(entry))
                .collect(Collectors.toList());

            Futures.serialize(posts)
                .ifSuccess(result -> {
                    System.out.println("Result of HTTP POST to the Orchestrator:");
                    for (var storeEntryList : result) {
                        System.out.println("  " + storeEntryList.asString());
                    }
                })
                .onFailure(Throwable::printStackTrace);
        })
        .onFailure(throwable -> {
            throwable.printStackTrace();
        });
    }

}