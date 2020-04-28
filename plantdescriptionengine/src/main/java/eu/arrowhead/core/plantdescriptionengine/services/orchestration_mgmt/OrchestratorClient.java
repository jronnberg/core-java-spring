package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
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

    private Future<StoreEntryListDto> postRule(Connection connection, List<PdeSystem> systems) {
        SystemPort producerPort = connection.producer();
        SystemPort consumerPort = connection.consumer();
        PdeSystem producer = null;
        PdeSystem consumer = null;

        for (PdeSystem system : systems) {
            String systemName = system.systemName();

            if (systemName.equals(producerPort.systemName())) {
                producer = system;
            } else if (systemName.equals(consumerPort.systemName())) {
                consumer = system;
            }
        }

        if (producer == null || consumer == null) {
            throw new NullPointerException("Port refers to non-existent system");
            // TODO: Remove this and instead validate all plant descriptions.
        }

        String serviceDefinitionName = null;

        for (Port port : producer.ports()) {
            if (port.portName().equals(producerPort.portName())) {
                serviceDefinitionName = port.serviceDefinition();
            }
        }

        if (serviceDefinitionName == null) {
            throw new NullPointerException("Service definition name not found");
            // TODO: Again, validate the plant description and remove this check.
        }

        return client.send(orchestratorAddress, new HttpClientRequest().method(HttpMethod.POST).uri("/orchestrator/mgmt/store")
            .body(DtoEncoding.JSON, List.of(new StoreRuleBuilder().serviceDefinitionName(serviceDefinitionName)
                .priority(1) // TODO: Change!
                .consumerSystemId(20) // TODO: Look up!
                .providerSystem(new ProviderSystemBuilder()
                    .systemName(producer.systemName())
                    .address("0.0.0.0") // TODO: Look up!
                    .port(28081) // TODO: Look up!
                    .build()
                )
                .serviceInterfaceName("HTTP-INSECURE-JSON")
                .cloud(cloud)
                .build())
            )
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, StoreEntryListDto.class));
    }

    private Future<Void> deleteRule(int id) {
        return client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.DELETE)
            .uri("/orchestrator/mgmt/store/" + id))
            .flatMap(response -> {
                return Future.done();
            });
    }

    private Future<Void> removeAllRules() {
        return getStoreEntries()
            .flatMap(entryList -> {
                if (entryList.count() == 0) {
                    System.out.println("No Orchestrator rules exist.");
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

    private Future<List<StoreRuleDto>> postRules(PlantDescriptionEntry entry) {
        var rulePosts = entry.connections()
            .stream()
            .map(connection -> postRule(connection, entry.systems()))
            .collect(Collectors.toList());

        return Futures.serialize(rulePosts)
            .flatMap(result -> {
                return Future.done();
            });
    }

    @Override
    public void onUpdate(List<PlantDescriptionEntryDto> entries) {
        removeAllRules()
        .ifSuccess(x -> {
            // For each Plant description entry, post its rules:
            // TODO: Only post rules for the active entry
            var tasks = entries.stream()
                .map(entry -> postRules(entry))
                .collect(Collectors.toList());

            Futures.serialize(tasks)
                .flatMap(result -> {
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