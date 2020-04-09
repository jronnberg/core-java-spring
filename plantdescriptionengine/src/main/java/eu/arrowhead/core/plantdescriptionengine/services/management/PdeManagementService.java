package eu.arrowhead.core.plantdescriptionengine.services.management;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;

import eu.arrowhead.core.plantdescriptionengine.requestvalidation.*;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.*;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.descriptor.SecurityDescriptor;
import se.arkalix.http.HttpStatus;
import se.arkalix.http.service.HttpService;
import se.arkalix.http.service.HttpServiceRequest;
import se.arkalix.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

public class PdeManagementService {

    private PlantDescriptionEntryStore entryStore;

    // Integer for storing the next plant description entry ID to be used:
    private static AtomicInteger nextId = new AtomicInteger(0);

    /**
     * @return A new Plant Description Entry ID.
     * TODO: Use some other implementation for this
     */
    private static int getNextId() {
        return nextId.incrementAndGet();
    }

    /**
     * Constructor of a PdeManagementService.
     * @param entryStore Storage object for keeping track of Plant Description
     *                   entries.
     */
    public PdeManagementService(PlantDescriptionEntryStore entryStore) {
        this.entryStore = entryStore;
    }

    /**
     * Handles an HTTP request to add a new Plant Description to the PDE.
     * @param request HTTP request containing a PlantDescription.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    private Future<HttpServiceResponse> onDescriptionPost(
        final HttpServiceRequest request, final HttpServiceResponse response
    ) {
        return request
            .bodyAs(PlantDescriptionDto.class)
            .map(description -> {
                final PlantDescriptionEntryDto entry = PlantDescriptionEntry.from(description, getNextId());
                try {
                    entryStore.put(entry);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return response
                    .status(HttpStatus.CREATED)
                    .body(entryStore.getListDto());
            });
    }

    /**
     * Handles an HTTP request to replace a Plant Description Entry in the PDE.
     * @param request HTTP request containing a PlantDescription.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    private Future<HttpServiceResponse> onDescriptionPut(
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
                    entryStore.put(entry);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
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
    private Future<HttpServiceResponse> onDescriptionPatch(
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

                final PlantDescriptionEntryDto entry = entryStore.get(id);

                if (entry == null) {
                    return response
                        .body("There is no plant description entry with ID " + idString + ".")
                        .status(HttpStatus.BAD_REQUEST);
                }

                final PlantDescriptionEntryDto updatedEntry = PlantDescriptionEntry.update(entry, newFields);

                try {
                    entryStore.put(updatedEntry);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }

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
    private Future<?> onDescriptionDelete(
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
            entryStore.remove(id);
        } catch (IOException e) {
            System.err.println(e);
            response.status(HttpStatus.INTERNAL_SERVER_ERROR);
            response.body("Encountered an error while deleting entry file.");
            return Future.done();
        }

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
    private Future<?> onDescriptionGet(
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

        final PlantDescriptionEntryDto entry = entryStore.get(id);

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
    private Future<?> onDescriptionsGet(
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
            System.err.println("Encountered the following error(s) while parsing an HTTP request: " +
                parser.getErrorMessage());
            return Future.done();
        }

        List<PlantDescriptionEntryDto> entries = entryStore.getEntries();

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
                .data(entries)
                .build());
        return Future.done();
    }

    /**
     * @return A HTTP Service that handles requests for retrieving and updating
     *         Plant Description data.
     */
    public HttpService getService() {
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
}
