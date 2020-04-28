package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

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
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.util.concurrent.Future;

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

    private void postRule(Connection connection, List<PdeSystem> systems) {
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

        client.send(orchestratorAddress, new HttpClientRequest().method(HttpMethod.POST).uri("/orchestrator/mgmt/store")
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
            .flatMap(response -> response.bodyAsString())
            .map(body -> {
                System.out.println("\nPOST result from orchestrator:");
                System.out.println(body);
                return null;
            }).onFailure(throwable -> {
                System.err.println("\nPOST failure:");
                throwable.printStackTrace();
            });
    }

    private Future<Void> deleteStoreEntry(int id) {
        return client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.DELETE)
            .uri("/orchestrator/mgmt/store/" + id))
            .flatMap(x -> {
                return Future.done();
            });
    }

    private Future<Void> removeAllRules() {
        return getStoreEntries()
            .flatMap(entryList -> {
                for (var entry : entryList.data()) {
                    System.out.println("Deleting entry " + entry.id());
                    deleteStoreEntry(entry.id())
                    .ifSuccess(x -> {
                        System.out.println("YES! KILLED " + entry.id());
                    }).wait();
                }
                System.out.println("Deleted all entries.");
                return Future.done();
            });

        /*
        .ifSuccess(body -> {
            System.err.println(body.asString());
        })
        .onFailure(throwable -> {
            throwable.printStackTrace();
        });
        System.out.println("Gererp");
        */

        /*
        return getStoreEntries().map(storeEntryList -> {
            for (var entry : storeEntryList.data()) {
                System.out.println("Destroying " + entry.id());
                deleteStoreEntry(entry.id()).wait();
            }
            return storeEntryList;
        });
        */
    }

    private void postRules(PlantDescriptionEntry entry) {
        List<PdeSystem> systems = entry.systems();
        for (Connection connection : entry.connections()) {
            postRule(connection, systems);
        }
    }

    @Override
    public void onUpdate(List<PlantDescriptionEntryDto> entries) {
        removeAllRules()
        .ifSuccess(x -> {
            // For each Plant description entry, post its rules:
            // TODO: Only post rules for the active entry
            for (var entry : entries) {
                postRules(entry);
            }
        })
        .onFailure(throwable -> {
            throwable.printStackTrace();
        });
    }

}