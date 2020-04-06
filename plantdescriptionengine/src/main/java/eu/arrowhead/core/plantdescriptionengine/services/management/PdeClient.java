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

    /**
     * @return An example plant description update.
     */
    public static PlantDescriptionUpdateDto createDescriptionUpdate() {

        PdePortDto serviceDiscoveryPort = new PdePortBuilder()
            .portName("service_discovery_updated")
            .serviceDefinition("Service Discovery Updated")
            .consumer(false)
            .build();

        PdePortDto authorizationPort = new PdePortBuilder()
            .portName("service_discovery_updated")
            .serviceDefinition("Service Discovery Updated")
            .consumer(false)
            .build();

        PdeConnectionEndpointDto consumer = new PdeConnectionEndpointBuilder()
            .systemName("Authorization Updated")
            .portName("service_discovery_updated")
            .build();

        PdeConnectionEndpointDto producer = new PdeConnectionEndpointBuilder()
            .systemName("Service Registry Updated")
            .portName("service_discovery_updated")
            .build();

        PdeConnectionDto connection = new PdeConnectionBuilder()
            .consumer(consumer)
            .producer(producer)
            .build();

        PdeSystemDto serviceRegistrySystem = new PdeSystemBuilder()
            .systemName("Service Registry Updated")
            .ports(Arrays.asList(serviceDiscoveryPort))
            .build();

        PdeSystemDto authorizationSystem = new PdeSystemBuilder()
            .systemName("Authorization Updated")
            .ports(Arrays.asList(authorizationPort))
            .build();

        PlantDescriptionUpdateDto update = new PlantDescriptionUpdateBuilder()
            .plantDescription("ArrowHead core updated")
            .active(false)
            .include(Collections.<Integer> emptyList())
            .systems(Arrays.asList(serviceRegistrySystem, authorizationSystem))
            .connections(Arrays.asList(connection))
            .build();

        return update;
    }

    private static void deleteDescription(InetSocketAddress address, String baseUri, HttpClient client, int descriptionId) {
        client.send(address, new HttpClientRequest()
                .method(HttpMethod.DELETE)
                .uri(baseUri + descriptionId))
                .map(body -> {
                    System.out.println("\nDELETE result:");
                    System.out.println(body);
                    return null;
                })
                .onFailure(throwable -> {
                    System.err.println("\nDELETE failure:");
                    throwable.printStackTrace();
                });
    }

    private static void getDescription(InetSocketAddress address, String baseUri, HttpClient client, int descriptionId) {
        client.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/pde/mgmt/pd/" + descriptionId)
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, PlantDescriptionEntryDto.class))
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

    private static void getDescriptions(InetSocketAddress address, String baseUri, HttpClient client) {
        client.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/pde/mgmt/pd")
            .queryParameter("page", "4")
            .queryParameter("item_per_page", "3")
            .queryParameter("sort_field", "createdAt")
            .queryParameter("direction", "DESC")
            .queryParameter("filter_field", "active")
            .queryParameter("filter_value", "false")
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

    private static void postDescription(InetSocketAddress address, String baseUri, HttpClient client) {
        client.send(address, new HttpClientRequest()
        .method(HttpMethod.POST)
        .uri(baseUri)
        .body(DtoEncoding.JSON, createDescription()))
        .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, PlantDescriptionEntryListDto.class))
        .map(body -> {
            System.out.println("\nPOST result:");
            System.out.println(body.asString());
            return null;
        })
        .onFailure(throwable -> {
            System.err.println("\nPOST failure:");
            throwable.printStackTrace();
        });
    }

    private static void putDescription(InetSocketAddress address, String baseUri, HttpClient client, int descriptionId) {
        client.send(address, new HttpClientRequest()
        .method(HttpMethod.PUT)
        .uri(baseUri + descriptionId)
        .body(DtoEncoding.JSON, createDescription()))
        .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, PlantDescriptionEntryDto.class))
        .map(body -> {
            System.out.println("\nPUT result:");
            System.out.println(body.asString());
            return null;
        })
        .onFailure(throwable -> {
            System.err.println("\nPUT failure:");
            throwable.printStackTrace();
        });
    }

    private static void patchDescription(InetSocketAddress address, String baseUri, HttpClient client, int descriptionId) {
        client.send(address, new HttpClientRequest()
        .method(HttpMethod.PATCH)
        .uri(baseUri + descriptionId)
        .body(DtoEncoding.JSON, createDescriptionUpdate()))
        .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, PlantDescriptionEntryDto.class))
        .map(body -> {
            System.out.println("\nPATCH result:");
            System.out.println(body.asString());
            return null;
        })
        .onFailure(throwable -> {
            System.err.println("\nPATCH failure:");
            throwable.printStackTrace();
        });
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
            String baseUri = "/pde/mgmt/pd/";

            getDescriptions(pdeSocketAddress, baseUri, client);
            Thread.sleep(1000);
            postDescription(pdeSocketAddress, baseUri, client);
            Thread.sleep(1000);
            postDescription(pdeSocketAddress, baseUri, client);
            Thread.sleep(1000);
            deleteDescription(pdeSocketAddress, baseUri, client, 0);
            Thread.sleep(1000);
            getDescriptions(pdeSocketAddress, baseUri, client);
            Thread.sleep(1000);
            putDescription(pdeSocketAddress, baseUri, client, 1);
            Thread.sleep(1000);
            getDescription(pdeSocketAddress, baseUri, client, 1);
            Thread.sleep(1000);
            patchDescription(pdeSocketAddress, baseUri, client, 1);

        }
        catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
