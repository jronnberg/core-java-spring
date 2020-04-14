package eu.arrowhead.core.plantdescriptionengine.services.monitor;

import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.descriptor.SecurityDescriptor;
import se.arkalix.http.HttpStatus;
import se.arkalix.http.service.HttpService;
import se.arkalix.http.service.HttpServiceRequest;
import se.arkalix.http.service.HttpServiceResponse;
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
     */
    public HttpService getService() {
        return new HttpService()
            .name("plant-description-management-service")
            .encodings(EncodingDescriptor.JSON)
            .security(SecurityDescriptor.CERTIFICATE)
            .basePath("/pde")
            .get("/monitor/alarm", (request, response) -> onAlarmsGet(request, response));
    }
}
