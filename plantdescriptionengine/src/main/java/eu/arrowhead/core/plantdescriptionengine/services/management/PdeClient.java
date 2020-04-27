package eu.arrowhead.core.plantdescriptionengine.services.management;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Path;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.*;

import se.arkalix.dto.DtoEncoding;
import se.arkalix.dto.DtoWriteException;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

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

        PortDto pdePort = new PortBuilder()
            .portName("pde_mgmt")
            .serviceDefinition("pde-mgmt")
            .consumer(false)
            .build();

        PortDto fakePort = new PortBuilder()
            .portName("pde_mgmt")
            .serviceDefinition("pde-mgmt")
            .consumer(false)
            .build();

        SystemPortDto consumer = new SystemPortBuilder()
            .systemName("fake_system")
            .portName("pde_mgmt")
            .build();

        SystemPortDto producer = new SystemPortBuilder()
            .systemName("sysop")
            .portName("pde_mgmt")
            .build();

        ConnectionDto connection = new ConnectionBuilder()
            .consumer(consumer)
            .producer(producer)
            .build();

        PdeSystemDto pde = new PdeSystemBuilder()
            .systemName("sysop")
            .ports(Arrays.asList(pdePort))
            .build();

        PdeSystemDto fakeSystem = new PdeSystemBuilder()
            .systemName("fake_system")
            .ports(Arrays.asList(fakePort))
            .build();

        PlantDescriptionDto description = new PlantDescriptionBuilder()
            .plantDescription("ArrowHead core")
            .active(true)
            .include(Arrays.asList(1,2,3))
            .systems(Arrays.asList(pde, fakeSystem))
            .connections(Arrays.asList(connection))
            .build();

        return description;
    }

    /**
     * @return An example plant description update.
     */
    public static PlantDescriptionUpdateDto createDescriptionUpdate() {

        PortDto serviceDiscoveryPort = new PortBuilder()
            .portName("service_discovery_updated")
            .serviceDefinition("Service Discovery Updated")
            .consumer(false)
            .build();

        PortDto authorizationPort = new PortBuilder()
            .portName("service_discovery_updated")
            .serviceDefinition("Service Discovery Updated")
            .consumer(false)
            .build();

        SystemPortDto consumer = new SystemPortBuilder()
            .systemName("Authorization Updated")
            .portName("service_discovery_updated")
            .build();

        SystemPortDto producer = new SystemPortBuilder()
            .systemName("Service Registry Updated")
            .portName("service_discovery_updated")
            .build();

        ConnectionDto connection = new ConnectionBuilder()
            .consumer(consumer)
            .producer(producer)
            .build();

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("a", "1");
        metadata.put("b", "2");
        metadata.put("c", "3");

        PdeSystemDto serviceRegistrySystem = new PdeSystemBuilder()
            .systemName("Service Registry Updated")
            .ports(Arrays.asList(serviceDiscoveryPort))
            .metadata(metadata)
            .build();

        PdeSystemDto authorizationSystem = new PdeSystemBuilder()
            .systemName("Authorization Updated")
            .ports(Arrays.asList(authorizationPort))
            .metadata(metadata)
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
            // .queryParameter("page", "4")
            // .queryParameter("item_per_page", "3")
            .queryParameter("sort_field", "createdAt")
            .queryParameter("direction", "DESC")
            // .queryParameter("filter_field", "active")
            // .queryParameter("filter_value", "false")
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
            .ifSuccess(body -> {
                System.err.println("\nPOST result:");
                System.err.println(body.asString());
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
            final var identity = new OwnedIdentity.Loader()
                .keyPassword(password)
                .keyStorePath(Path.of(args[0]))
                .keyStorePassword(password)
                .load();
            final var trustStore = TrustStore.read(Path.of(args[1]), password);
            Arrays.fill(password, '\0');

            // Create Arrowhead client.
            final var client = new HttpClient.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .build();

            final var pdeSocketAddress = new InetSocketAddress("localhost", 28081);
            String baseUri = "/pde/mgmt/pd/";

            getDescriptions(pdeSocketAddress, baseUri, client);
            Thread.sleep(1000);
            postDescription(pdeSocketAddress, baseUri, client);
            // Thread.sleep(1000);
            // postDescription(pdeSocketAddress, baseUri, client);
            // Thread.sleep(1000);
            // deleteDescription(pdeSocketAddress, baseUri, client, 0);
            // Thread.sleep(1000);
            // getDescriptions(pdeSocketAddress, baseUri, client);
            // Thread.sleep(1000);
            // putDescription(pdeSocketAddress, baseUri, client, 1);
            // Thread.sleep(1000);
            // getDescription(pdeSocketAddress, baseUri, client, 1);
            // Thread.sleep(1000);
            // patchDescription(pdeSocketAddress, baseUri, client, 0);
        }
        catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
