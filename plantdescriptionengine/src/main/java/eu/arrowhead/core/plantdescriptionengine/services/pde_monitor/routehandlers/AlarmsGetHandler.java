package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;


import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to retrieve PDE alarms.
 */
public class AlarmsGetHandler implements HttpRouteHandler {

    /**
     * Handles an HTTP call to acquire a list of alarms raised by the PDE.
     *
     * @param request  HTTP request object.
     * @param response HTTP response object.
     */
    @Override
    public Future<?> handle(final HttpServiceRequest request, final HttpServiceResponse response) throws Exception {

        response.status(HttpStatus.OK).body();
        return Future.done();
    }
}