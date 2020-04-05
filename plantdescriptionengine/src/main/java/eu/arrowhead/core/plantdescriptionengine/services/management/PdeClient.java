package eu.arrowhead.core.plantdescriptionengine.services.management;

import java.util.Arrays;
import java.util.Collections;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.*;

import java.nio.file.Path;

import se.arkalix.dto.DtoEncoding;
import se.arkalix.http.HttpMethod;
import se.arkalix.http.client.HttpClient;
import se.arkalix.http.client.HttpClientRequest;
import se.arkalix.security.X509KeyStore;
import se.arkalix.security.X509TrustStore;

import java.net.InetSocketAddress;

/**
 * Simple HttpClient used in the development of the plant description engine.
 *
 * TODO: Remove this class!
 */
public class PdeClient {

    /**
     * @return An example plant description.
     */
    public static PlantDescriptionDto createDescription() {

        PdePortDto serviceDiscoveryPort = new PdePortBuilder()
            .portName("service_discovery")
            .serviceDefinition("Service Discovery")
            .consumer(false)
            .build();

        PdePortDto authorizationPort = new PdePortBuilder()
            .portName("service_discovery")
            .serviceDefinition("Service Discovery")
            .consumer(false)
            .build();

        PdeConnectionEndpointDto consumer = new PdeConnectionEndpointBuilder()
            .systemName("Authorization")
            .portName("service_discovery")
            .build();

        PdeConnectionEndpointDto producer = new PdeConnectionEndpointBuilder()
            .systemName("Service Registry")
            .portName("service_discovery")
            .build();

        PdeConnectionDto connection = new PdeConnectionBuilder()
            .consumer(consumer)
            .producer(producer)
            .build();

        PdeSystemDto serviceRegistrySystem = new PdeSystemBuilder()
            .systemName("Service Registry")
            .ports(Arrays.asList(serviceDiscoveryPort))
            .build();

        PdeSystemDto authorizationSystem = new PdeSystemBuilder()
            .systemName("Authorization")
            .ports(Arrays.asList(authorizationPort))
            .build();

        PlantDescriptionDto description = new PlantDescriptionBuilder()
            .plantDescription("ArrowHead core")
            .active(true)
            .include(Collections.<Integer> emptyList())
            .systems(Arrays.asList(serviceRegistrySystem, authorizationSystem))
            .connections(Arrays.asList(connection))
            .build();

        return description;
    }

    public static void main(final String[] args) {
        if (args.length != 2) {
            System.err.println("Requires two command line arguments: <keyStorePath> and <trustStorePath>");
            System.exit(1);
        }
        try {
            System.out.println("Running plant description client...");

            // Load keystore and truststore.
            // The key store represents the systems own identity, while the
            // trust store represents all identities that are to be trusted.
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var keyStore = new X509KeyStore.Loader()
                .keyPassword(password)
                .keyStorePath(Path.of(args[0]))
                .keyStorePassword(password)
                .load();
            final var trustStore = X509TrustStore.read(Path.of(args[1]), password);

            // Create Arrowhead client.
            final var client = new HttpClient.Builder()
                .keyStore(keyStore)
                .trustStore(trustStore)
                .build();

            final var pdeSocketAddress = new InetSocketAddress("localhost", 28081);
            PlantDescriptionDto description = createDescription();
            String uri = "/pde/mgmt/pd";

            // Post a plant description
            client.send(pdeSocketAddress, new HttpClientRequest()
                .method(HttpMethod.POST)
                .uri(uri)
                .body(DtoEncoding.JSON, description))
                .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, PlantDescriptionEntryDto.class))
                .map(body -> {
                    System.out.println("\nPOST result:");
                    System.out.println(body.asString());
                    return null;
                })
                .onFailure(throwable -> {
                    System.err.println("\nPOST failure:");
                    throwable.printStackTrace();
                });

            // Get current plant descriptions
            client.send(pdeSocketAddress, new HttpClientRequest()
                .method(HttpMethod.GET)
                .uri("/pde/mgmt/pd")
                .queryParameter("page", "4")
                .queryParameter("item_per_page", "3")
                .queryParameter("sort_field", "createdAt")
                .queryParameter("direction", "DESC")
                .queryParameter("filter_field", "active")
                .queryParameter("filter_value", "falsse")
                .header("accept", "application/json"))
                .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, PlantDescriptionEntryListDto.class))
                .map(body -> {
                    System.out.println("\nGET result:");
                    System.out.println(body.asString());
                    return null;
                })
                .onFailure(throwable -> {
                    System.err.println("\nGET failure:");
                    throwable.printStackTrace();
                });
        }
        catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
