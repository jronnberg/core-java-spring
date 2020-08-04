package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to retrieve a specific Plant Description Entries.
 */
public class GetPlantDescription implements HttpRouteHandler {

    private final PlantDescriptionTracker entryMap;

    /**
     * Class constructor
     *
     * @param entryMap Object that keeps track of Plant Description Enties.
     */
    public GetPlantDescription(PlantDescriptionTracker entryMap) {
        Objects.requireNonNull(entryMap, "Expected Plant Description Entry map");
        this.entryMap = entryMap;
    }

    /**
     * Handles an HTTP call to acquire the PlantDescriptionEntry specified by
     * the id path parameter.
     *
     * @param request  HTTP request object.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    @Override
    public Future<?> handle(final HttpServiceRequest request, final HttpServiceResponse response) throws Exception {

        String idString = request.pathParameter(0);
        int id;

        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            response.status(HttpStatus.BAD_REQUEST);
            response.body(ErrorMessage.of(idString + " is not a valid Plant Description Entry ID."));
            response.status(HttpStatus.BAD_REQUEST);
            return Future.done();
        }

        final PlantDescriptionEntryDto entry = entryMap.get(id);

        if (entry == null) {
            response.body(ErrorMessage.of("Plant Description with ID " + id + " not found."));
            response.status(HttpStatus.NOT_FOUND);
            return Future.done();
        }

        response.status(HttpStatus.OK).body(entry);
        return Future.done();
    }
}