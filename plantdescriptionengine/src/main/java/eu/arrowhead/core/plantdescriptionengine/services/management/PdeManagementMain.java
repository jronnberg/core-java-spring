package eu.arrowhead.core.plantdescriptionengine.services.management;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.arrowhead.core.plantdescriptionengine.requestvalidation.BooleanParameter;
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

    private static PlantDescriptionEntryListDto getCurrentEntryList() {
        List<PlantDescriptionEntryDto> entryList = new ArrayList<>(entriesById.values());
        return new PlantDescriptionEntryListBuilder()
            .count(entryList.size()) // TODO: This shouldn't be necessary
            .data(entryList)
            .build();
    }

    /**
     * @return The path to use for writing Plant Description Entries to disk.
     */
    private static String getFilename(int entryId) {
        return DESCRIPTION_DIRECTORY + entryId + ".json";
    }

    private static void writeToFile(final PlantDescriptionEntryDto entry) throws DtoWriteException, IOException {
        final String filename = getFilename(entry.id());
        final FileOutputStream out = new FileOutputStream(new File(filename));
        final DtoWriter writer = new DtoWriter(out);
        entry.writeJson(writer);
        out.close();
    }

    private static void deleteEntryFile(int entryId) throws IOException {
        final String filename = getFilename(entryId);
        Files.delete(Paths.get(filename));
    }

    /**
     * @param password Password of the private key associated with the
     *                 certificate in key store.
     * @param keyStorePath Path to the keystore representing the systems own
     *                     identity.
     * @param trustStorePath Path to the trust store representing all identities
     *                       that are to be trusted by the system.
     * @return An Arrowhead Framework system.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private static ArSystem getArSystem(final char[] password, final String keyStorePath, final String trustStorePath)
            throws GeneralSecurityException, IOException {

        X509KeyStore keyStore = null;
        X509TrustStore trustStore = null;

        keyStore = new X509KeyStore.Loader()
            .keyPassword(password)
            .keyStorePath(Path.of(keyStorePath))
            .keyStorePassword(password).load();
        trustStore = X509TrustStore.read(Path.of(trustStorePath), password);

        final var system = new ArSystem.Builder()
            .keyStore(keyStore)
            .trustStore(trustStore)
            .localPort(28081)
            .build();

        return system;
    }

    /**
     * Handles an HTTP request to add a new Plant Description to the PDE.
     * @param request HTTP request containing a PlantDescription.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    private static Future<HttpServiceResponse> onDescriptionPost(
        final HttpServiceRequest request, final HttpServiceResponse response
    ) {
        return request
            .bodyAs(PlantDescriptionDto.class)
            .map(description -> {
                final PlantDescriptionEntryDto entry = PlantDescriptionEntry.from(description, getNextId());
                try {
                    writeToFile(entry);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                entriesById.put(entry.id(), entry);
                return response
                    .status(HttpStatus.CREATED)
                    .body(getCurrentEntryList());
            });
    }

    /**
     * Handles an HTTP request to replace a Plant Description Entry in the PDE.
     * @param request HTTP request containing a PlantDescription.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    private static Future<HttpServiceResponse> onDescriptionPut(
        final HttpServiceRequest request, final HttpServiceResponse response
    ) {
        return request
            .bodyAs(PlantDescriptionDto.class)
            .map(description -> {
                int id;

                try {
                    id = Integer.parseInt(request.pathParameter(0));
                } catch (NumberFormatException e) {
                    response.status(HttpStatus.BAD_REQUEST);
                    response.body(request.pathParameter(0) + " is not a valid plant description entry ID.");
                    return response.status(HttpStatus.BAD_REQUEST);
                }

                final PlantDescriptionEntryDto entry = PlantDescriptionEntry.from(description, id);

                try {
                    writeToFile(entry);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                entriesById.put(entry.id(), entry);
                return response
                    .status(HttpStatus.CREATED)
                    .body(entry);
            });
    }

    /**
     * Handles an HTTP request to update the Plant Description Entry specified
     * by the id parameter with the information in the PlantDescriptionUpdate
     * parameter.
     * @param request HTTP request containing a PlantDescriptionUpdate.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    private static Future<HttpServiceResponse> onDescriptionPatch(
        final HttpServiceRequest request, final HttpServiceResponse response
    ) {
        return request
            .bodyAs(PlantDescriptionUpdateDto.class)
            .map(newFields -> {
                String idString = request.pathParameter(0);
                int id;

                try {
                    id = Integer.parseInt(idString);
                } catch (NumberFormatException e) {
                    response.status(HttpStatus.BAD_REQUEST);
                    response.body(idString + " is not a valid plant description entry ID.");
                    return response.status(HttpStatus.BAD_REQUEST);
                }

                final PlantDescriptionEntryDto entry = entriesById.get(id);

                if (entry == null) {
                    return response
                        .body("There is no plant description entry with ID " + idString + ".")
                        .status(HttpStatus.BAD_REQUEST);
                }

                final PlantDescriptionEntryDto updatedEntry = PlantDescriptionEntry.update(entry, newFields);

                try {
                    writeToFile(updatedEntry);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                entriesById.put(updatedEntry.id(), updatedEntry);
                return response
                    .status(HttpStatus.CREATED)
                    .body(updatedEntry);
            });
    }

    /**
     * Handles an HTTP request to delete an existing Plant Description from the
     * PDE.
     * @param request HTTP request containing a PlantDescription.
     * @param response HTTP response object.
     */
    private static Future<?> onDescriptionDelete(
        final HttpServiceRequest request, final HttpServiceResponse response
    ) {
        int id;

        try {
            id = Integer.parseInt(request.pathParameter(0));
        } catch (NumberFormatException e) {
            response.status(HttpStatus.BAD_REQUEST);
            response.body(request.pathParameter(0) + " is not a valid plant description entry ID.");
            return Future.done();
        }

        try {
            deleteEntryFile(id);
        } catch (IOException e) {
            System.err.println(e);
            response.status(HttpStatus.INTERNAL_SERVER_ERROR);
            response.body("Encountered an error while deleting entry file.");
            return Future.done();
        }

        entriesById.remove(id);

        response.status(HttpStatus.OK);
        response.body("ok");

        return Future.done();
    }

    /**
     * Handles an HTTP call to acquire the PlantDescriptionEntry specified by
     * the id path parameter.
     * @param request HTTP request object.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    private static Future<?> onDescriptionGet(
        final HttpServiceRequest request, final HttpServiceResponse response
    ) {

        String idString = request.pathParameter(0);
        int id;

        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            response.status(HttpStatus.BAD_REQUEST);
            response.body(idString + " is not a valid plant description entry ID.");
            response.status(HttpStatus.BAD_REQUEST);
            return Future.done();
        }

        final PlantDescriptionEntryDto entry = entriesById.get(id);

        if (entry == null) {
            response.body("There is no plant description entry with ID " + idString + ".");
            response.status(HttpStatus.BAD_REQUEST);
            return Future.done();
        }

        response.status(HttpStatus.OK).body(entry);
        return Future.done();
    }

    /**
     * Handles an HTTP call to acquire a list of Plant Description Entries
     * present in the PDE.
     * @param request HTTP request object.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    private static Future<?> onDescriptionsGet(
        final HttpServiceRequest request, final HttpServiceResponse response
    ) {
        final List<QueryParameter> requiredParameters = null;
        final List<QueryParameter> acceptedParameters = Arrays.asList(
            new IntParameter("page")
                .requires(new IntParameter("item_per_page")),
            new StringParameter("sort_field")
                .legalValues(Arrays.asList("id", "createdAt", "updatedAt")),
            new StringParameter("direction")
                .legalValues(Arrays.asList("ASC", "DESC"))
                .setDefault("ASC"),
            new StringParameter("filter_field")
                .legalValue("active")
                .requires(new BooleanParameter("filter_value"))
        );

        final var parser = new QueryParamParser(requiredParameters, acceptedParameters, request);

        if (parser.hasError()) {
            response.status(HttpStatus.BAD_REQUEST);
            response.body(parser.getErrorMessage());
            System.err.println("Encountered the following error(s) while parsing an HTTP request: " + parser.getErrorMessage());
            return Future.done();
        }

        List<PlantDescriptionEntryDto> entries = new ArrayList<>(entriesById.values());

        final Optional<String> sortField = parser.getString("sort_field");
        if (sortField.isPresent()) {
            final String sortDirection = parser.getString("direction").get();
            final boolean sortAscending = (sortDirection.equals("ASC") ? true : false);
            PlantDescriptionEntry.sort(entries, sortField.get(), sortAscending);
        }

        final Optional<Integer> page = parser.getInt("page");
        if (page.isPresent()) {
            int itemsPerPage = parser.getInt("item_per_page").get();
            int from = Math.min(page.get() * itemsPerPage, 0);
            int to = Math.min(from + itemsPerPage, entries.size());
            entries = entries.subList(from, to);
        }

        if (parser.getString("filter_field").isPresent()) {
            boolean filterValue = parser.getBoolean("filter_value").get();
            PlantDescriptionEntry.filterOnActive(entries, filterValue);
        }

        response
            .status(HttpStatus.OK)
            .body(new PlantDescriptionEntryListBuilder()
                .count(entries.size()) // TODO: This shouldn't be necessary
                .data(entries)
                .build());
        return Future.done();
    }

    private static HttpService getServices() {
        return new HttpService()
            .name("plant-description-management-service")
            .encodings(EncodingDescriptor.JSON)
            .security(SecurityDescriptor.CERTIFICATE)
            .basePath("/pde")
            .get("/mgmt/pd/#id", (request, response) -> onDescriptionGet(request, response))
            .get("/mgmt/pd", (request, response) -> onDescriptionsGet(request, response))
            .post("/mgmt/pd", (request, response) -> onDescriptionPost(request, response))
            .delete("/mgmt/pd/#id", (request, response) -> onDescriptionDelete(request, response))
            .put("/mgmt/pd/#id", (request, response) -> onDescriptionPut(request, response))
            .patch("/mgmt/pd/#id", (request, response) -> onDescriptionPatch(request, response));
    }

    public static void main(final String[] args) {

        if (args.length != 2) {
            System.err.println("Requires two command line arguments: <keyStorePath> and <trustStorePath>");
            System.exit(1);
        }

        final File directory = new File(DESCRIPTION_DIRECTORY);
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
