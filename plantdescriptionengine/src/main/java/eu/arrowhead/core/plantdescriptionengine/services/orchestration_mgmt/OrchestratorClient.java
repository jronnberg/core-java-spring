package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.ProviderSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreRuleBuilder;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;

public class OrchestratorClient {

    private final HttpClient client;
    private final InetSocketAddress orchestratorAddress = new InetSocketAddress("localhost", 8441); // TODO: Remove hardcoded address

    public OrchestratorClient(HttpClient client) {
        this.client = client;
    }

	public void postRule() {
        client.send(orchestratorAddress, new HttpClientRequest()
            .method(HttpMethod.POST)
            .uri("/orchestrator/mgmt/store")
            .body(DtoEncoding.JSON, List.of(
                new StoreRuleBuilder()
                    .serviceDefinitionName("echo")
                    .priority(1)
                    .consumerSystemId(9)
                    .providerSystem(new ProviderSystemBuilder()
                        .systemName("echo")
                        .address("127.0.0.1")
                        .port(5001)
                        .build())
                    .serviceInterfaceName("/echo")
                    .cloud(new CloudBuilder()
                        .name("Xarepo")
                        .operator("Xarepo")
                        .build())
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
                System.err.println("\nGET failure:");
                throwable.printStackTrace();
            });
        }

}