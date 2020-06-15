package eu.arrowhead.core.plantdescriptionengine.services;

import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import se.arkalix.dto.DtoReadException;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpCatcherHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

public class DtoReadExceptionCatcher implements HttpCatcherHandler<DtoReadException> {

    @Override
    public Future<?> handle(DtoReadException throwable, HttpServiceRequest request, HttpServiceResponse response)
            throws Exception {
        response.status(HttpStatus.BAD_REQUEST)
            .body(ErrorMessage.of(throwable.getMessage()));
        return Future.done();
    }

}