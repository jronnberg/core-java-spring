package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitorable.routehandlers;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitorable.dto.InventoryIdBuilder;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

public class GetInventoryId implements HttpRouteHandler {

    @Override
    public Future<HttpServiceResponse> handle(
        HttpServiceRequest request,
        HttpServiceResponse response
    ) {
        response
            .status(HttpStatus.OK)
            .body(new InventoryIdBuilder().build());
        return Future.done();
    }

}
