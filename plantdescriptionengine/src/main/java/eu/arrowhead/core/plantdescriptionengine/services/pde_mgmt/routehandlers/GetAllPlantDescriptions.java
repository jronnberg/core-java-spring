package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.requestvalidation.BooleanParameter;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.IntParameter;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.ParseError;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParamParser;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParameter;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.StringParameter;
import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryListBuilder;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to retrieve all current Plant Description Entries.
 */
public class GetAllPlantDescriptions implements HttpRouteHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetAllPlantDescriptions.class);

    private final PlantDescriptionTracker pdTracker;

    /**
     * Class constructor
     *
     * @param pdTracker Object that keeps track of Plant Description Enties.
     */
    public GetAllPlantDescriptions(PlantDescriptionTracker pdTracker) {
        Objects.requireNonNull(pdTracker, "Expected Plant Description Entry map");
        this.pdTracker = pdTracker;
    }

    /**
     * Handles an HTTP request to acquire a list of Plant Description Entries
     * present in the PDE.
     *
     * @param request HTTP request object.
     * @param response HTTP response whose body contains a list of Plant
     *                 Description entries.
     */
    @Override
    public Future<?> handle(final HttpServiceRequest request, final HttpServiceResponse response) throws Exception {
        final List<QueryParameter> requiredParameters = null;
        final List<QueryParameter> acceptedParameters = List.of(
            new IntParameter("page")
                .min(0)
                .requires(new IntParameter("item_per_page")),
            new StringParameter("sort_field")
                .legalValues(List.of("id", "createdAt", "updatedAt")),
            new StringParameter("direction")
                .legalValues(List.of("ASC", "DESC"))
                .setDefault("ASC"),
            new StringParameter("filter_field")
                .legalValue("active")
                .requires(new BooleanParameter("filter_value"))
        );

        QueryParamParser parser;

        try {
            parser = new QueryParamParser(requiredParameters, acceptedParameters, request);
        } catch(ParseError error) {
            response.status(HttpStatus.BAD_REQUEST);
            response.body(ErrorMessage.of(error.getMessage()));
            logger.error("Encountered the following error(s) while parsing an HTTP request: " +
                 error.getMessage());
            return Future.done();
        }

        List<PlantDescriptionEntryDto> entries = pdTracker.getEntries();

        final Optional<String> sortField = parser.getString("sort_field");
        if (sortField.isPresent()) {
            final String sortDirection = parser.getString("direction").get();
            final boolean sortAscending = (sortDirection.equals("ASC") ? true : false);
            PlantDescriptionEntry.sort(entries, sortField.get(), sortAscending);
        }

        final Optional<Integer> page = parser.getInt("page");
        if (page.isPresent()) {
            int itemsPerPage = parser.getInt("item_per_page").get();
            int from = Math.max(page.get() * itemsPerPage, 0);
            int to = Math.min(from + itemsPerPage, entries.size());
            entries = entries.subList(from, to);
        }

        if (parser.getString("filter_field").isPresent()) {
            boolean filterValue = parser.getBoolean("filter_value").get();
            PlantDescriptionEntry.filterByActive(entries, filterValue);
        }

        response
            .status(HttpStatus.OK)
            .body(new PlantDescriptionEntryListBuilder()
                .data(entries)
                .count(entries.size())
                .build());
        return Future.done();
    }
}