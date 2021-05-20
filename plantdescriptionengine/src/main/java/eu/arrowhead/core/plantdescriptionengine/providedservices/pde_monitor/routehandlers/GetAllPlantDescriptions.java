package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import eu.arrowhead.core.plantdescriptionengine.MonitorInfo;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.MonitorPlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PlantDescriptionEntryListDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.BooleanParameter;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.IntParameter;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.ParseError;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.QueryParamParser;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.QueryParameter;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.StringParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.codec.CodecType;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles HTTP requests to retrieve all current Plant Description Entries.
 */
public class GetAllPlantDescriptions implements HttpRouteHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetAllPlantDescriptions.class);

    // Filter fields
    private static final String ACTIVE = "active";
    private static final String ITEM_PER_PAGE = "item_per_page";
    private static final String PAGE = "page";

    // Sort fields and values
    private static final String SORT_FIELD = "sort_field";
    private static final String ID = "id";
    private static final String CREATED_AT = "createdAt";
    private static final String UPDATED_AT = "updatedAt";
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";
    private static final String DIRECTION = "direction";

    private final PlantDescriptionTracker pdTracker;
    private final MonitorInfo monitorInfo;

    /**
     * Class constructor
     *
     * @param monitorInfo Object that stores information on monitorable
     *                    systems.
     * @param pdTracker   Object that stores information on Plant Description
     *                    entries.
     */
    public GetAllPlantDescriptions(final MonitorInfo monitorInfo, final PlantDescriptionTracker pdTracker) {
        Objects.requireNonNull(monitorInfo, "Expected MonitorInfo");
        Objects.requireNonNull(pdTracker, "Expected Plant Description Tracker");

        this.monitorInfo = monitorInfo;
        this.pdTracker = pdTracker;
    }

    /**
     * Handles an HTTP request to acquire a list of Plant Description Entries
     * present in the PDE.
     *
     * @param request  HTTP request object.
     * @param response HTTP response whose body contains a list of Plant
     *                 Description entries.
     */
    @Override
    public Future<HttpServiceResponse> handle(final HttpServiceRequest request, final HttpServiceResponse response) {
        Objects.requireNonNull(request, "Expected request.");
        Objects.requireNonNull(response, "Expected response.");

        final IntParameter itemPerPageParam = new IntParameter.Builder()
            .name(ITEM_PER_PAGE)
            .min(0)
            .build();
        final IntParameter pageParam = new IntParameter.Builder()
            .name(PAGE)
            .min(0)
            .requires(itemPerPageParam)
            .build();
        final StringParameter sortFieldParam = new StringParameter.Builder()
            .name(SORT_FIELD)
            .legalValues(ID, CREATED_AT, UPDATED_AT)
            .build();
        final StringParameter directionParam = new StringParameter.Builder()
            .name(DIRECTION)
            .legalValues(ASC, DESC)
            .defaultValue(ASC)
            .build();

        final BooleanParameter activeParam = new BooleanParameter.Builder()
            .name(ACTIVE)
            .build();

        final List<QueryParameter> acceptedParameters = List.of(pageParam, sortFieldParam, directionParam, activeParam);

        final QueryParamParser parser;

        try {
            parser = new QueryParamParser(null, acceptedParameters, request);
        } catch (final ParseError error) {
            logger.error("Encountered the following error(s) while parsing an HTTP request: " + error.getMessage());

            response
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorMessage.of(error.getMessage()), CodecType.JSON);
            return Future.success(response);

        }

        List<eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto> entries = pdTracker
            .getEntries();

        final Optional<String> sortField = parser.getValue(sortFieldParam);
        if (sortField.isPresent()) {
            final String sortDirection = parser.getRequiredValue(directionParam);
            final boolean sortAscending = "ASC".equals(sortDirection);
            switch (sortField.get()) {
                case ID:
                    PlantDescriptionEntry.sortById(entries, sortAscending);
                    break;
                case CREATED_AT:
                    PlantDescriptionEntry.sortByCreatedAt(entries, sortAscending);
                    break;
                case UPDATED_AT:
                    PlantDescriptionEntry.sortByUpdatedAt(entries, sortAscending);
                    break;
            }
        }

        final Optional<Integer> page = parser.getValue(pageParam);
        if (page.isPresent()) {
            final int itemsPerPage = parser.getRequiredValue(itemPerPageParam);
            final int from = Math.min(page.get() * itemsPerPage, entries.size());
            final int to = Math.min(from + itemsPerPage, entries.size());
            entries = entries.subList(from, to);
        }

        final Optional<Boolean> active = parser.getValue(activeParam);
        if (active.isPresent()) {
            PlantDescriptionEntry.filterByActive(entries, active.get());
        }

        // Extend each Plant Description Entry with monitor data retrieved from
        // monitorable services:
        final List<MonitorPlantDescriptionEntryDto> extendedEntries = new ArrayList<>();
        for (final PlantDescriptionEntry entry : entries) {
            extendedEntries.add(DtoUtils.extend(entry, monitorInfo, pdTracker));
        }

        PlantDescriptionEntryListDto result = new PlantDescriptionEntryListDto.Builder()
            .data(extendedEntries)
            .count(extendedEntries.size())
            .build();

        response
            .status(HttpStatus.OK)
            .body(result, CodecType.JSON);

        return Future.success(response);
    }

}