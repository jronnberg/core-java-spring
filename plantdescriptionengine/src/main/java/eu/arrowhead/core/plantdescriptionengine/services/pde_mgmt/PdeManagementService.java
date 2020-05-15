package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import eu.arrowhead.core.plantdescriptionengine.requestvalidation.*;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.*;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;

public class PdeManagementService {
    private static final Logger logger = LoggerFactory.getLogger(PdeManagementService.class);

    private final PlantDescriptionEntryMap entryMap;

    /**
     * Constructor of a PdeManagementService.
     *
     * @param entryMap An object that maps ID:s to Plant Description
     *                            Entries.
     */
    public PdeManagementService(PlantDescriptionEntryMap entryMap) {
        Objects.requireNonNull(entryMap, "Expected plant description map");
        this.entryMap = entryMap;
    }

    /**
     * Handles an HTTP request to add a new Plant Description to the PDE.
     *
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
                final PlantDescriptionEntryDto entry = PlantDescriptionEntry.from(description, entryMap.getUniqueId());

                try {
                    entryMap.put(entry);
                } catch (final BackingStoreException e) {
                    logger.error("Failure when communicating with backing store.", e);
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return response
                    .status(HttpStatus.CREATED)
                    .body(entryMap.getListDto()); // TODO: Respond with the created entry instead?
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
                    entryMap.put(entry);
                } catch (final BackingStoreException e) {
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

                final PlantDescriptionEntryDto entry = entryMap.get(id);

                if (entry == null) {
                    return response
                        .status(HttpStatus.BAD_REQUEST)
                        .body("There is no plant description entry with ID " + idString + ".");
                }

                final PlantDescriptionEntryDto updatedEntry = PlantDescriptionEntry.update(entry, newFields);

                try {
                    entryMap.put(updatedEntry);
                } catch (final BackingStoreException e) {
                    e.printStackTrace();
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                return response
                    .status(HttpStatus.OK)
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

        if (entryMap.get(id) == null) {
            response.status(HttpStatus.NOT_FOUND);
            response.body("Plant Description with ID " + id + " not found.");
            return Future.done();
        }

        try {
            entryMap.remove(id);
        } catch (BackingStoreException e) {
            logger.error("Failed to remove Plant Description Entry from backing store", e);
            response.status(HttpStatus.INTERNAL_SERVER_ERROR);
            response.body("Encountered an error while deleting entry file.");
            return Future.done();
        }

        response.status(HttpStatus.OK);
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
            logger.error("Encountered the following error(s) while parsing an HTTP request: " +
                parser.getErrorMessage());
            return Future.done();
        }

        List<PlantDescriptionEntryDto> entries = entryMap.getEntries();

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
            .count(entries.size())
            .build());
        return Future.done();
    }

    /**
     * @return A HTTP Service that handles requests for retrieving and updating
     *         Plant Description data.
     *
     * @param secure Indicates whether the returned service should run in secure
     *               mode.
     */
    public HttpService getService(boolean secure) {
        HttpService service = new HttpService()
            .name("pde-mgmt")
            .encodings(EncodingDescriptor.JSON)
            .basePath("/pde")
            .get("/mgmt/pd/#id", new OnDescriptionsGet(entryMap))
            .get("/mgmt/pd", (request, response) -> onDescriptionsGet(request, response))
            .post("/mgmt/pd", (request, response) -> onDescriptionPost(request, response))
            .delete("/mgmt/pd/#id", (request, response) -> onDescriptionDelete(request, response))
            .put("/mgmt/pd/#id", (request, response) -> onDescriptionPut(request, response))
            .patch("/mgmt/pd/#id", (request, response) -> onDescriptionPatch(request, response));

        if (secure) {
            service.accessPolicy(AccessPolicy.cloud());
        } else {
            service.accessPolicy(AccessPolicy.unrestricted());
        }

        return service;
    }

    /**
     * @return A HTTP Service that handles requests for retrieving and updating
     *         Plant Description data, running in secure mode.
     */
    public HttpService getService() {
        return getService(true);
    }

}