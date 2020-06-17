package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionValidator;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to update or create Plant Description Entries.
 */
public class ReplacePlantDescription implements HttpRouteHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReplacePlantDescription.class);

    private final PlantDescriptionEntryMap entryMap;

    /**
     * Class constructor
     *
     * @param entryMap Object that keeps track of Plant Description Enties.
     */
    public ReplacePlantDescription(PlantDescriptionEntryMap entryMap) {
        Objects.requireNonNull(entryMap, "Expected Plant Description Entry map");
        this.entryMap = entryMap;
    }

    /**
     * Handles an HTTP request to update or create the Plant Description Entry.
     *
     * @param request HTTP request containing the ID of the entry to
     *                create/update, and a {@link PlantDescriptionUpdate}
     *                describing its new state.
     * @param response HTTP response containing the created/updated entry.
     */
    @Override
    public Future<?> handle(final HttpServiceRequest request, final HttpServiceResponse response) throws Exception {
        return request
            .bodyAs(PlantDescriptionDto.class)
            .map(description -> {
                int id;

                try {
                    id = Integer.parseInt(request.pathParameter(0));
                } catch (NumberFormatException e) {
                    return response
                        .status(HttpStatus.BAD_REQUEST)
                        .body(request.pathParameter(0) + " is not a valid Plant Description Entry ID.");
                }

                final PlantDescriptionEntryDto entry = PlantDescriptionEntry.from(description, id);
                final var validator = new PlantDescriptionValidator(entry);
                if (validator.hasError()) {
                    return response
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ErrorMessage.of(validator.getErrorMessage()));
                }

                try {
                    entryMap.put(entry);
                } catch (final BackingStoreException e) {
                    logger.error("Failed to write Plant Description Entry update to backing store.", e);
                    return response.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return response
                    .status(HttpStatus.CREATED)
                    .body(entry);
            });
    }
}