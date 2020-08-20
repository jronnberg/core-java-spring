package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

import java.util.List;
import java.util.Optional;

import eu.arrowhead.core.plantdescriptionengine.requestvalidation.IntParameter;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParamParser;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParameter;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.StringParameter;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmListBuilder;
import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to retrieve PDE alarms.
 */
public class GetAllPdeAlarms implements HttpRouteHandler {

    /**
     * Handles an HTTP call to acquire a list of PDE alarms raised by the PDE.
     *
     * @param request HTTP request object.
     * @param response HTTP response containing an alarm list.
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
            new StringParameter("filter_field") // TODO: Remove filter_field, replace with severity, cleared, etc?
                .legalValues(List.of("systemName", "severity")) // TODO: Add "acknowledged"
                .requires(new StringParameter("filter_value")) // TODO: An incorrect value results in a server failure at the moment.
        );

        final var parser = new QueryParamParser(requiredParameters, acceptedParameters, request);
        List<PdeAlarmDto> alarms = Locator.getAlarmManager().getAlarms();

        final Optional<String> filterField = parser.getString("filter_field");

        final Optional<String> sortField = parser.getString("sort_field");
        if (sortField.isPresent()) {
            final String sortDirection = parser.getString("direction").get();
            final boolean sortAscending = (sortDirection.equals("ASC") ? true : false);
            PdeAlarm.sort(alarms, sortField.get(), sortAscending);
        }

        final Optional<Integer> page = parser.getInt("page");
        if (page.isPresent()) {
            int itemsPerPage = parser.getInt("item_per_page").get();
            int from = Math.max(page.get() * itemsPerPage, 0);
            int to = Math.min(from + itemsPerPage, alarms.size());
            alarms = alarms.subList(from, to);
        }

        if (filterField.isPresent()) {
            String filterValue = parser.getString("filter_value").get();
            if (filterField.get().equals("systemName")) {
                PdeAlarm.filterBySystemName(alarms, filterValue);
            } else if (filterField.get().equals("severity")) {
                PdeAlarm.filterBySeverity(alarms, filterValue);
            }
        }

        response.body(new PdeAlarmListBuilder()
            .data(alarms)
            .count(alarms.size())
            .build());
        response.status(HttpStatus.OK);
        return Future.done();
    }
}