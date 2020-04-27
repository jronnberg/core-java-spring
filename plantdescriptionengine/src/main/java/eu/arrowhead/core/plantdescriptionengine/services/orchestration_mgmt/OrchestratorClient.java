package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.Connection;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PdeSystem;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.Port;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.SystemPort;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.ProviderSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreRuleBuilder;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;

public class OrchestratorClient {

    private final HttpClient client;
    private final InetSocketAddress orchestratorAddress = new InetSocketAddress("localhost", 8441); // TODO: Remove hardcoded address
    private final CloudDto cloud;

    public OrchestratorClient(HttpClient client, CloudDto cloud) {
        Objects.requireNonNull(client, "Expected HttpClient");
        Objects.requireNonNull(cloud, "Expected CloudDto");

        this.client = client;
        this.cloud = cloud;
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

        System.out.println("Provider systemName: " + producer.systemName());
        System.out.println("serviceDefinitionName: " + serviceDefinitionName);

        client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.POST)
            .uri("/orchestrator/mgmt/store")
            .body(DtoEncoding.JSON, List.of(
                new StoreRuleBuilder()
                    .serviceDefinitionName(serviceDefinitionName)
                    .priority(1) // TODO: Change!
                    .consumerSystemId(20) // TODO: Look up!
                    .providerSystem(new ProviderSystemBuilder()
                        .systemName(producer.systemName())
                        .address("0.0.0.0") // TODO: Look up!
                        .port(28081) // TODO: Look up!
                        .build())
                    .serviceInterfaceName("HTTP-INSECURE-JSON")
                    .cloud(cloud)
                    .build()
            ))
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsString())
            .map(body -> {
                System.out.println("\nPOST result from orchestrator:");
                System.out.println(body);
                return null;
            })
            .onFailure(throwable -> {
                System.err.println("\nPOST failure:");
                throwable.printStackTrace();
            });
    }

	public void onPlantDescriptionUpdate(PlantDescriptionEntry entry) {

        List<PdeSystem> systems = entry.systems();
        for (Connection connection : entry.connections()) {
            postRule(connection, systems);
        }
    }

}