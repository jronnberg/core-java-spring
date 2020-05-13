package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;

public class PdeMonitorService {

    /**
     * Handles an HTTP call to acquire a list of PDE alarms raised by the PDE.
     * @param request HTTP request object.
     * @param response HTTP response containing an alarm list object.
     */
    private Future<?> onAlarmsGet(
        final HttpServiceRequest request, final HttpServiceResponse response
    ) {
        response.status(HttpStatus.OK).body("ok");
        return Future.done();
    }

    /**
     * @return A HTTP Service used to monitor alarms raised by the Plant
     *         Description Engine core system.
     *
     * @param insecure Indicates whether the returned service should run in
     *                 secure mode.
     */
    public HttpService getService(boolean secure) {

        var service = new HttpService()
            .name("plant-description-monitor-service")
            .encodings(EncodingDescriptor.JSON)
            .basePath("/pde")
            .get("/monitor/alarm", (request, response) -> onAlarmsGet(request, response));

        if (secure) {
            service.accessPolicy(AccessPolicy.cloud());
        } else {
            service.accessPolicy(AccessPolicy.unrestricted());
        }

        return service;
    }

    /**
     * @return A HTTP Service used to monitor alarms raised by the Plant
     *         Description Engine core system, running in secure mode.
     */
    public HttpService getService() {
        return getService(true);
    }
}
