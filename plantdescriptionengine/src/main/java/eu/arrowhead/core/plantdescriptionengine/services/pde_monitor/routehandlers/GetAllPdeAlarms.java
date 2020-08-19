package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

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
        final var alarms = Locator.getAlarmManager().getAlarms();
        response.body(new PdeAlarmListBuilder()
            .data(alarms)
            .count(alarms.size())
            .build());
        response.status(HttpStatus.OK);
        return Future.done();
    }
}