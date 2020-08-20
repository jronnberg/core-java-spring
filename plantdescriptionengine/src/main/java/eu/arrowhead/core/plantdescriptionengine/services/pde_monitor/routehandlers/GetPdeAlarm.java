package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmDto;
import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to retrieve a specific PDE Alarm.
 */
public class GetPdeAlarm implements HttpRouteHandler {

    /**
     * Handles an HTTP call to acquire the PDE Alarm specified by the id path
     * parameter.
     *
     * @param request  HTTP request object.
     * @param response HTTP response containing the current
     *                 PlantDescriptionEntryList.
     */
    @Override
    public Future<?> handle(final HttpServiceRequest request, final HttpServiceResponse response) throws Exception {

        String idString = request.pathParameter(0);
        int id;

        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            response.status(HttpStatus.BAD_REQUEST);
            response.body(ErrorMessage.of(idString + " is not a valid PDE Alarm ID."));
            response.status(HttpStatus.BAD_REQUEST);
            return Future.done();
        }

        final PdeAlarmDto alarm = Locator.getAlarmManager().getAlarm(id);

        if (alarm == null) {
            response.body(ErrorMessage.of("PDE Alarm with ID " + id + " not found."));
            response.status(HttpStatus.NOT_FOUND);
            return Future.done();
        }

        response.status(HttpStatus.OK).body(alarm);
        return Future.done();
    }
}