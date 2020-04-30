package eu.arrowhead.core.plantdescriptionengine.services.management;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.net.InetSocketAddress;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.*;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.ServiceRegistryEntryListDto;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystemDto;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.dto.DtoReadException;
import se.arkalix.dto.binary.ByteArrayReader;
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

    private static final String defaultdescriptionFilepath = "demo-data/description_1.json";
    private static final String defaultUpdateFilepath = "demo-data/update_1.json";

    public static PlantDescriptionDto readDescription(String filename) {
        byte[] bytes = null;
        PlantDescriptionDto description = null;
        try {
            bytes = Files.readAllBytes(Paths.get(filename));
            description = PlantDescriptionDto.readJson(new ByteArrayReader(bytes));
        } catch (DtoReadException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return description;
    }

    public static PlantDescriptionDto readDescription() {
        return readDescription(defaultdescriptionFilepath);
    }

    public static PlantDescriptionUpdateDto readUpdate(String filename) {
        byte[] bytes = null;
        PlantDescriptionUpdateDto update = null;
        try {
            bytes = Files.readAllBytes(Paths.get(filename));
            update = PlantDescriptionUpdateDto.readJson(new ByteArrayReader(bytes));
        } catch (DtoReadException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return update;
    }

    public static PlantDescriptionUpdateDto readUpdate() {
        return readUpdate(defaultUpdateFilepath);
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
            .body(DtoEncoding.JSON, readDescription()))
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
        .body(DtoEncoding.JSON, readDescription()))
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
            .body(DtoEncoding.JSON, readUpdate()))
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
            // postDescription(pdeSocketAddress, baseUri, client);
            // Thread.sleep(1000);
            // postDescription(pdeSocketAddress, baseUri, client);
            // Thread.sleep(1000);
            // deleteDescription(pdeSocketAddress, baseUri, client, 0);
            // Thread.sleep(1000);
            // getDescriptions(pdeSocketAddress, baseUri, client);
            // Thread.sleep(1000);
            putDescription(pdeSocketAddress, baseUri, client, 1);
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
