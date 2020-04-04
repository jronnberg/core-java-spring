package eu.arrowhead.core.plantdescriptionengine.services.management;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import eu.arrowhead.core.plantdescriptionengine.requestvalidation.IntParameter;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParameter;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParamParser;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.StringParameter;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.*;
import se.arkalix.ArSystem;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.descriptor.SecurityDescriptor;
import se.arkalix.http.HttpStatus;
import se.arkalix.http.service.HttpService;
import se.arkalix.http.service.HttpServiceRequest;
import se.arkalix.http.service.HttpServiceResponse;
import se.arkalix.dto.DtoWriteException;
import se.arkalix.security.X509KeyStore;
import se.arkalix.security.X509TrustStore;
import se.arkalix.util.concurrent.Future;

import java.util.ArrayList;
import java.util.Arrays;

public class PdeManagementMain {

    // ID to use for the next plant description entry:
    private static int nextId = 0;

    /**
     * @return A to use for the next plant description that is created.
     */
    private static int getNextId() {
        return nextId++;
    }

    // File path to the directory for storing JSON representations of plant
    // descriptions:
    final static String DESCRIPTION_DIRECTORY = "plant-descriptions/";

    private static Map<Integer, PlantDescriptionEntryDto> entriesById = new HashMap<>();

    private static void writeToFile(PlantDescriptionEntryDto entry) throws DtoWriteException, IOException {
        final String filename = DESCRIPTION_DIRECTORY + entry.id() + ".json";
        final FileOutputStream out = new FileOutputStream(new File(filename));
        final DtoWriter writer = new DtoWriter(out);
        entry.writeJson(writer);
        out.close();
    }

    /**
     * @param password       Password of the private key associated with the
     *                       certificate in key store.
     * @param keyStorePath   Path to the keystore representing the systems own
     *                       identity.
     * @param trustStorePath Path to the trust store representing all identities
     *                       that are to be trusted by the system.
     * @return An Arrowhead Framework system.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private static ArSystem getArSystem(char[] password, String keyStorePath, String trustStorePath)
            throws GeneralSecurityException, IOException {

        X509KeyStore keyStore = null;
        X509TrustStore trustStore = null;

        keyStore = new X509KeyStore.Loader()
            .keyPassword(password)
            .keyStorePath(Path.of(keyStorePath))
            .keyStorePassword(password).load();
        trustStore = X509TrustStore.read(Path.of(trustStorePath), password);

        var system = new ArSystem.Builder()
            .keyStore(keyStore)
            .trustStore(trustStore)
            .localPort(28081)
            .build();

        return system;
    }

    /**
     * Handles a HTTP request to add a new Plant Description to the PDE.
     * @param request HTTP request containing a PlantDescription.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    private static Future<HttpServiceResponse> onDescriptionPost(
        HttpServiceRequest request, HttpServiceResponse response
    ) {
        return request
            .bodyAs(PlantDescriptionDto.class)
            .map(description -> {
                PlantDescriptionEntryDto entry = PlantDescriptionEntry.from(description, getNextId());
                try {
                    writeToFile(entry);
                } catch (IOException e) {
                    e.printStackTrace();
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                entriesById.put(entry.id(), entry);
                return response.status(HttpStatus.CREATED).body(entry);
            });
    }

    /**
     * Handles a HTTP call to acquire a list of Plant Description Entries
     * present in the PDE.
     * @param request HTTP request object.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    private static Future<?> handleGetEntries(
        HttpServiceRequest request, HttpServiceResponse response
    ) {

        List<QueryParameter> requiredParameters = Arrays.asList(
            new IntParameter.Builder()
                .name("page")
                .ifPresentRequire("item_per_page")
                .build(),
            new IntParameter.Builder()
                 .name("item_per_page")
                 .build()
        );

        List<QueryParameter> acceptedParameters = new ArrayList<>();

        var parser = new QueryParamParser(requiredParameters, acceptedParameters);

        if (!parser.parse(request)) {
            response.status(HttpStatus.BAD_REQUEST);
            response.body(parser.getErrorMessage());
            return Future.done(); // Or failure? Error message to user?
        }

        List<PlantDescriptionEntryDto> entryList = new ArrayList<>(entriesById.values());

        Optional<Integer> maybePage = parser.getInt("page");
        if (maybePage.isPresent()) {
            int page = maybePage.get();
            int itemsPerPage = parser.getInt("item_per_page").get();
            int from = page * itemsPerPage;
            int to = from + itemsPerPage;
            from = Math.min(from, 0);
            to = Math.min(to, entryList.size());
            entryList = entryList.subList(from, to);
        }

        response
            .status(HttpStatus.OK)
            .body(
                new PlantDescriptionEntryListBuilder()
                .count(entryList.size()) // TODO: This shouldn't be necessary?
                .data(entryList)
                .build()
            );
        return Future.done();
    }

    private static HttpService getServices() {
        return new HttpService()
            .name("plant-description-management-service")
            .encodings(EncodingDescriptor.JSON)
            .security(SecurityDescriptor.CERTIFICATE)
            .basePath("/pde")
            .get("/mgmt/pd", (request, response) -> handleGetEntries(request, response))
            .post("/mgmt/pd", (request, response) -> onDescriptionPost(request, response));
    }

    public static void main(final String[] args) {

        if (args.length != 2) {
            System.err.println("Requires two command line arguments: <keyStorePath> and <trustStorePath>");
            System.exit(1);
        }

        File directory = new File(DESCRIPTION_DIRECTORY);
        if (!directory.exists()){
            directory.mkdir();
        }

        final var password = new char[] { '1', '2', '3', '4', '5', '6' };
        ArSystem system = null;

        try {
            system = getArSystem(password, args[0], args[1]);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(74);
        }

        System.out.println("Providing services...");
        system.provide(getServices())
            .onFailure(Throwable::printStackTrace);
    }
}
