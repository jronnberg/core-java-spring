package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionUpdateDto;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to update Plant Description Entries.
 */
public class UpdatePlantDescription implements HttpRouteHandler {
    private static final Logger logger = LoggerFactory.getLogger(UpdatePlantDescription.class);

    private final PlantDescriptionEntryMap entryMap;

    /**
     * Class constructor
     *
     * @param entryMap Object that keeps track of Plant Description Enties.
     */
    public UpdatePlantDescription(PlantDescriptionEntryMap entryMap) {
        Objects.requireNonNull(entryMap, "Expected Plant Description Entry map");
        this.entryMap = entryMap;
    }

    /**
     * Handles an HTTP request to update the Plant Description Entry specified
     * by the id parameter with the information in the PlantDescriptionUpdate
     * parameter.
     *
     * @param request HTTP request containing a PlantDescriptionUpdate.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList. // TODO: Return the patched entry only?
     */
    @Override
    public Future<?> handle(final HttpServiceRequest request, final HttpServiceResponse response) throws Exception {
        return request
            .bodyAs(PlantDescriptionUpdateDto.class)
            .map(newFields -> {
                String idString = request.pathParameter(0);
                int id;

                try {
                    id = Integer.parseInt(idString);
                } catch (NumberFormatException e) {
                    response.status(HttpStatus.BAD_REQUEST);
                    response.body(idString + " is not a valid Plant Description Entry ID.");
                    return response.status(HttpStatus.BAD_REQUEST);
                }

                final PlantDescriptionEntryDto entry = entryMap.get(id);

                if (entry == null) {
                    return response
                        .status(HttpStatus.NOT_FOUND)
                        .body("Plant Description with ID " + idString + " not found.");
                }

                final PlantDescriptionEntryDto updatedEntry = PlantDescriptionEntry.update(entry, newFields);

                try {
                    entryMap.put(updatedEntry);
                } catch (final BackingStoreException e) {
                    logger.error("Failed to write Plant Description Entry update to backing store.", e);
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                return response
                    .status(HttpStatus.OK)
                    .body(updatedEntry);
            });
    }
}