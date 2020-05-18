package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandler;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to create Plant Description Entries.
 */
public class DescriptionPostHandler implements HttpRouteHandler {
    private static final Logger logger = LoggerFactory.getLogger(DescriptionPostHandler.class);

    private final PlantDescriptionEntryMap entryMap;

    /**
     * Class constructor
     *
     * @param entryMap Object that keeps track of Plant Description Enties.
     */
    public DescriptionPostHandler(PlantDescriptionEntryMap entryMap) {
        Objects.requireNonNull(entryMap, "Expected Plant Description Entry map");
        this.entryMap = entryMap;
    }

    /**
     * Handles an HTTP request to add a new Plant Description to the PDE.
     *
     * @param request  HTTP request object containing a Plant Description.
     * @param response HTTP response containing the newly created Plant
     *                 Description entry.
     */
    @Override
    public Future<?> handle(final HttpServiceRequest request, final HttpServiceResponse response) throws Exception {
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
                    .body(entry);
            });
    }
}