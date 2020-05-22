package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.services.monitorable.dto.InventoryIdDto;
import eu.arrowhead.core.plantdescriptionengine.services.monitorable.dto.SystemDataDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers.GetAllPdeAlarms;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers.GetAllPlantDescriptions;
import se.arkalix.ArServiceHandle;
import se.arkalix.ArSystem;
import se.arkalix.description.ServiceDescription;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.query.ServiceQuery;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Futures;

public class PdeMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(PdeMonitorService.class);

    private final ArSystem arSystem;
    private final PlantDescriptionEntryMap entryMap;
    private final HttpClient httpClient; // TODO: Remove this?
    private final boolean secure;
    private final ServiceQuery monitorableQuery;

    private final static int pollInterval = 5000; // Milliseconds

    /**
     * Class constructor.
     *
     * @param arSystem An Arrowhead Framework system used to provide this
     *                 service.
     * @param entryMap An object that maps ID:s to Plant Description
     *                 Entries.
     * @param insecure Indicates whether the service should run in secure mode.
     */
    public PdeMonitorService(
        ArSystem arSystem, PlantDescriptionEntryMap entryMap, HttpClient httpClient, boolean secure
    ) {
        Objects.requireNonNull(arSystem, "Expected AR System");
        Objects.requireNonNull(entryMap, "Expected plant description map");
        Objects.requireNonNull(httpClient, "Expected HTTP client");

        this.arSystem = arSystem;
        this.entryMap = entryMap;
        this.httpClient = httpClient;
        this.secure = secure;

        monitorableQuery = arSystem.consume()
            .name("monitorable")
            .encodings(EncodingDescriptor.JSON);
    }

    /**
     * Registers this service with an Arrowhead system, eventually making it
     * accessible to remote Arrowhead systems.
     *
     * @return A HTTP Service used to monitor alarms raised by the Plant
     *         Description Engine core system.
     *
     */
    public Future<ArServiceHandle> provide() {

        var service = new HttpService()
            .name("plant-description-monitor-service")
            .encodings(EncodingDescriptor.JSON)
            .basePath("/pde/monitor")
            .get("/pd", new GetAllPlantDescriptions(monitorableQuery, httpClient, entryMap))
            .get("/alarm", new GetAllPdeAlarms());

        if (secure) {
            service.accessPolicy(AccessPolicy.cloud());
        } else {
            service.accessPolicy(AccessPolicy.unrestricted());
        }

        monitorLoop();

        return arSystem.provide(service);
    }

    private void monitorLoop() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                pollMonitorableSystems()
                    .onFailure(e -> {
                        e.printStackTrace();
                        logger.error("Polling of monitorable systems failed.", e);
                    });
            }
        }, 0, pollInterval);
    }

    private Future<?> pollMonitorableSystems() { // TODO: Change return type
        return monitorableQuery.resolveAll()
            .flatMap(services -> {

                var idRequests = services.stream()
                    .map(service -> retrieveId(service))
                    .collect(Collectors.toList());

                var dataRequests = services.stream()
                    .map(service -> retrieveSystemData(service))
                    .collect(Collectors.toList());

                var serialIdRequest = Futures.serialize(idRequests)
                .flatMap(inventoryIds -> {
                    for (var id : inventoryIds) {
                        System.out.println(id.id());
                    }
                    return Future.done();
                });

                var serialDataRequest = Futures.serialize(dataRequests)
                .flatMap(systemDataList -> {
                    for (var systemData : systemDataList) {
                        System.out.println(systemData.data());
                    }
                    return Future.done();
                });

                return Futures.serialize(List.of(serialIdRequest, serialDataRequest));

        });
    }

    private Future<InventoryIdDto> retrieveId(ServiceDescription service) {
        var address = service.provider().socketAddress();
        return httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/monitorable/inventoryid")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, InventoryIdDto.class));
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
