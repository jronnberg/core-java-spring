package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.services.monitorable.dto.InventoryIdDto;
import eu.arrowhead.core.plantdescriptionengine.services.monitorable.dto.SystemDataDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import se.arkalix.description.ServiceDescription;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.query.ServiceQuery;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Futures;

/**
 * Handles HTTP requests to retrieve all current Plant Description Entries.
 */
public class GetAllPlantDescriptions implements HttpRouteHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetAllPlantDescriptions.class);

    private final PlantDescriptionEntryMap entryMap;
    private final ServiceQuery monitorableQuery;
    private final HttpClient httpClient; // TODO: Remove this?

    /**
     * Class constructor
     *
     * @param monitorableQuery Object used to query the Arrowhead system for
     *                         monitorable services.
     * @param httpClient Object for communicating with monitorable services.
     * @param entryMap Object that keeps track of Plant Description Enties.
     *
     */
    public GetAllPlantDescriptions(
        ServiceQuery monitorableQuery, HttpClient httpClient, PlantDescriptionEntryMap entryMap
    ) {
        Objects.requireNonNull(monitorableQuery, "Expected Service query");
        Objects.requireNonNull(httpClient, "Expected HTTP client");
        Objects.requireNonNull(entryMap, "Expected Plant Description Entry Map");

        this.monitorableQuery = monitorableQuery;
        this.httpClient = httpClient;
        this.entryMap = entryMap;
    }

    /**
     * Class constructor
     *
     * @param entryMap Object that keeps track of Plant Description Enties.
     */
    /*
    public DescriptionsGetHandler(PlantDescriptionEntryMap entryMap) {
        Objects.requireNonNull(entryMap, "Expected Plant Description Entry map");
        this.entryMap = entryMap;
    }
    */

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

    return monitorableQuery.resolveAll()
        .flatMap(services -> {

            var idRequests = services.stream()
                .map(service -> retrieveId(service))
                .collect(Collectors.toList());

            /*var dataRequests = services.stream()
                .map(service -> retrieveSystemData(service))
                .collect(Collectors.toList());*/

            return Futures.serialize(idRequests)
                .flatMap(inventoryIds -> {
                    for (var id : inventoryIds) {
                        System.out.println(id.id());
                    }
                    response.status(HttpStatus.OK).body("ok");
                    return Future.done();
                    /*return Futures.serialize(dataRequests)
                        .map(dataList -> {
                            for (var systemData : dataList) {
                                System.out.println(systemData.data());
                            }
                            response.status(HttpStatus.OK).body("ok");
                            return Future.done();
                        });*/
                });
        });
    }

        /*
        return monitorableQuery.using(HttpConsumer.factory())
            .flatMap(consumer -> consumer.send(new HttpConsumerRequest()
                .method(HttpMethod.GET)
                .uri("/monitorable/inventoryid")))
                .flatMap(result -> result.bodyAs(InventoryIdDto.class))
                .flatMap(inventoryId -> {
                    response.status(HttpStatus.OK).body(inventoryId);
                    return Future.done();
                });
        */

    private Future<InventoryIdDto> retrieveId(ServiceDescription service) {
        var address = service.provider().socketAddress();
        return httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/monitorable/inventoryid")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, InventoryIdDto.class));

            /*
            .flatMap(inventoryId -> {
                System.out.println(inventoryId.id());
                System.out.println(service.provider().name());

                for (var entry : entryMap.getEntries()) {
                    for (var system : entry.systems()) {
                        // TODO: What if there is no system name?
                        System.out.println(system.systemName());
                        if (!system.systemName().isPresent()) {
                            continue;
                        }
                        var systemName = system.systemName().get();
                        if (systemName.equals(service.provider().name())) {
                            System.out.println("Matches " + system.systemName());
                        }
                    }
                }

                return Future.done();
            });
        */
    }

    private Future<SystemDataDto> retrieveSystemData(ServiceDescription service) {
        var address = service.provider().socketAddress();
        return httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/monitorable/systemdata")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, SystemDataDto.class));
    }

}