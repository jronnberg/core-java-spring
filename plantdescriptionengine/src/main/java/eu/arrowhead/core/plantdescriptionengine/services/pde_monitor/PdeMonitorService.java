package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

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

public class PdeMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(PdeMonitorService.class);

    private final ArSystem arSystem;
    private final PlantDescriptionEntryMap entryMap;
    private final HttpClient httpClient; // TODO: Remove this?
    private final boolean secure;
    private final ServiceQuery monitorableQuery;
    private final MonitorInfo monitorInfo = new MonitorInfo();

    private final static int pollInterval = 3000; // Milliseconds

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
            .get("/pd", new GetAllPlantDescriptions(monitorInfo, entryMap))
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
                pollMonitorableSystems();
            }
        }, 0, pollInterval);
    }

    private void pollMonitorableSystems() {
        monitorableQuery.resolveAll()
            .ifSuccess(services -> {
                for (var service : services) {
                    retrieveId(service);
                    retrieveSystemData(service);
                }
            })
            .onFailure(e -> {
                logger.error("Failed to poll monitorable systems.", e);
            });
    }

    private void retrieveId(ServiceDescription service) {
        final String providerName = service.provider().name();
        final var address = service.provider().socketAddress();

        httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/monitorable/inventoryid")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, InventoryIdDto.class))
            .ifSuccess(inventoryId -> {
                monitorInfo.putInventoryId(providerName, inventoryId.id());
            })
            .onFailure(e -> {
                monitorInfo.removeInventoryId(providerName);
                // TODO: Error handling, raise an alarm?
            });
    }

    private void retrieveSystemData(ServiceDescription service) {
        final String providerName = service.provider().name();
        final var address = service.provider().socketAddress();

        httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/monitorable/systemdata")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, SystemDataDto.class))
            .ifSuccess(systemData -> {
                monitorInfo.putSystemData(providerName, systemData.data());
            })
            .onFailure(e -> {
                monitorInfo.removeSystemData(providerName);
                // TODO: Error handling, raise an alarm?
            });
    }

}
