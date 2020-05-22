package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryList;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.MonitorInfo;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to retrieve all current Plant Description Entries.
 */
public class GetAllPlantDescriptions implements HttpRouteHandler {

    private final PlantDescriptionEntryMap entryMap;
    private final MonitorInfo monitorInfo;

    /**
     * Class constructor
     *
     * @param httpClient Object for communicating with monitorable services.
     * @param monitorInfo Object that keeps track information on monitorable
     *                    systems.
     *
     */
    public GetAllPlantDescriptions(MonitorInfo monitorInfo, PlantDescriptionEntryMap entryMap
    ) {
        Objects.requireNonNull(monitorInfo, "Expected MonitorInfo");
        Objects.requireNonNull(entryMap, "Expected Plant Description Entry Map");

        this.monitorInfo = monitorInfo;
        this.entryMap = entryMap;
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
        var body = PlantDescriptionEntryList.extend(entryMap.getListDto(), monitorInfo);
        response.status(HttpStatus.OK).body(body);
        return Future.done();
    }

}