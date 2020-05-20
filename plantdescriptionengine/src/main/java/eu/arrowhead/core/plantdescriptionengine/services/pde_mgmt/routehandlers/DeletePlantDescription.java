package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStoreException;
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

    private final PlantDescriptionEntryMap entryMap;

    /**
     * Class constructor
     *
     * @param entryMap Object that keeps track of Plant Description Enties.
     */
    public DeletePlantDescription(PlantDescriptionEntryMap entryMap) {
        Objects.requireNonNull(entryMap, "Expected Plant Description Entry map");
        this.entryMap = entryMap;
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
            response.status(HttpStatus.BAD_REQUEST);
            response.body(request.pathParameter(0) + " is not a valid Plant Description Entry ID.");
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
}