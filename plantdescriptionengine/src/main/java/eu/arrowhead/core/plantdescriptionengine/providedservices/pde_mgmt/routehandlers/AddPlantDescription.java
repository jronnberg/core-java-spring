package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.PlantDescriptionValidator;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to create Plant Description Entries.
 */
public class AddPlantDescription implements HttpRouteHandler {
    private static final Logger logger = LoggerFactory.getLogger(AddPlantDescription.class);

    private final PlantDescriptionTracker PdTracker;

    /**
     * Class constructor
     *
     * @param pdTracker Object that keeps track of Plant Description Entries.
     */
    public AddPlantDescription(PlantDescriptionTracker pdTracker) {
        Objects.requireNonNull(pdTracker, "Expected Plant Description Tracker");
        this.PdTracker = pdTracker;
    }

    /**
     * Handles an HTTP request to add a new Plant Description to the PDE.
     *
     * @param request  HTTP request object containing a Plant Description.
     * @param response HTTP response containing the newly created Plant Description
     *                 entry.
     */
    @Override
    public Future<?> handle(final HttpServiceRequest request, final HttpServiceResponse response) throws Exception {
        return request.bodyAs(PlantDescriptionDto.class).map(description -> {

            final PlantDescriptionEntryDto entry = PlantDescriptionEntry.from(description, PdTracker.getUniqueId());
            final var validator = new PlantDescriptionValidator(entry);
            if (validator.hasError()) {
                return response
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorMessage.of(validator.getErrorMessage()));
            }

            try {
                PdTracker.put(entry);
            } catch (final PdStoreException e) {
                logger.error("Failure when communicating with backing store.", e);
                return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return response.status(HttpStatus.CREATED).body(entry);
        });
    }
}