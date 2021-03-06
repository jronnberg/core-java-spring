package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to delete Plant Description Entries.
 */
public class DeletePlantDescription implements HttpRouteHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeletePlantDescription.class);

    private final PlantDescriptionTracker pdTracker;

    /**
     * Class constructor
     *
     * @param pdTracker Object that keeps track of Plant Description Entries.
     */
    public DeletePlantDescription(PlantDescriptionTracker pdTracker) {
        Objects.requireNonNull(pdTracker, "Expected Plant Description Entry Tracker");
        this.pdTracker = pdTracker;
    }

    /**
     * Handles an HTTP request to delete an existing Plant Description from the
     * PDE.
     * @param request HTTP request containing a PlantDescription.
     * @param response HTTP response object.
     */
    @Override
    public Future<?> handle(final HttpServiceRequest request, final HttpServiceResponse response) throws Exception {

        int id;

        try {
            id = Integer.parseInt(request.pathParameter(0));
        } catch (NumberFormatException e) {
            final String errMsg = "'" + request.pathParameter(0) + "' is not a valid Plant Description Entry ID.";
            response.body(DtoEncoding.JSON, ErrorMessage.of(errMsg));
            response.status(HttpStatus.BAD_REQUEST);
            return Future.done();
        }

        if (pdTracker.get(id) == null) {
            response.status(HttpStatus.NOT_FOUND);
            response.body(ErrorMessage.of("Plant Description with ID " + id + " not found."));
            return Future.done();
        }

        try {
            pdTracker.remove(id);
        } catch (PdStoreException e) {
            logger.error("Failed to remove Plant Description Entry from backing store", e);
            response.status(HttpStatus.INTERNAL_SERVER_ERROR);
            response.body(ErrorMessage.of("Encountered an error while deleting entry file."));
            return Future.done();
        }

        response.status(HttpStatus.OK);
        return Future.done();
    }
}