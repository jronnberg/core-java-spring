package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

import java.util.List;
import java.util.Optional;

import eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParamParser;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParameter;
import eu.arrowhead.core.plantdescriptionengine.requestvalidation.StringParameter;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarm;
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
            new StringParameter("filter_field")
                .legalValues(List.of("systemName", "severity"))
                .requires(new StringParameter("filter_value"))
        );

        final var parser = new QueryParamParser(requiredParameters, acceptedParameters, request);
        final var alarms = Locator.getAlarmManager().getAlarms();

        final Optional<String> filterField = parser.getString("filter_field");

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