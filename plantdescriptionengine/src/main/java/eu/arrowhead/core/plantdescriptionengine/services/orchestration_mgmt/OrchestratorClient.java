package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import eu.arrowhead.core.plantdescriptionengine.services.management.PlantDescriptionUpdateListener;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.Connection;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PdeSystem;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.Port;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.SystemPort;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.ProviderSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryListDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreRuleBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreRuleDto;
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
        final List<PdeSystem> systems = entry.systems();
        final SystemPort producerPort = connection.producer();
        final SystemPort consumerPort = connection.consumer();

        PdeSystem producer = null;
        PdeSystem consumer = null;

        // Find the consumer and producer systems involved.
        for (PdeSystem system : systems) {
            String systemName = system.systemName();

            if (systemName.equals(producerPort.systemName())) {
                producer = system;
            } else if (systemName.equals(consumerPort.systemName())) {
                consumer = system;
            }
        }

        // TODO: Remove this and instead validate all plant descriptions.
        Objects.requireNonNull(producer, "Port refers to non-existent producer system");
        Objects.requireNonNull(consumer, "Port refers to non-existent consumer system");

        // Find the service definition name.
        String serviceDefinitionName = null;

        for (Port port : producer.ports()) {
            if (port.portName().equals(producerPort.portName())) {
                serviceDefinitionName = port.serviceDefinition();
            }
        }

        // TODO: Again, validate the plant description and remove this check.
        Objects.requireNonNull(serviceDefinitionName, "Expected service definition name");

        // TODO: Remove these hard-coded values! ------------------------------>
        int consumerId = 20;
        String providerAddress = "0.0.0.0";
        int providerPort = 28081;
        int priority = 1;
        String serviceInterfaceName = "HTTP-INSECURE-JSON";
        // <--------------------------------------------------------------------

        return new StoreRuleBuilder()
            .serviceDefinitionName(serviceDefinitionName)
            .priority(priority)
            .consumerSystemId(consumerId)
            .providerSystem(new ProviderSystemBuilder()
                .systemName(producer.systemName())
                .address(providerAddress)
                .port(providerPort)
                .build()
            )
            .serviceInterfaceName(serviceInterfaceName)
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
                .flatMap(postResult -> {
                    System.out.println("Orchestration rules for all entries posted.");
                    return Future.done();
                })
                .onFailure(Throwable::printStackTrace);
        })
        .onFailure(throwable -> {
            throwable.printStackTrace();
        });
    }

}