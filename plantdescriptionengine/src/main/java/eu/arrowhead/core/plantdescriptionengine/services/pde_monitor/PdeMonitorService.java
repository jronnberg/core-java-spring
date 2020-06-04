package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers.GetAllPdeAlarms;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers.GetAllPlantDescriptions;
import se.arkalix.ArServiceHandle;
import se.arkalix.ArSystem;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;

public class PdeMonitorService {

    private final ArSystem arSystem;
    private final MonitorableClient monitorableClient;
    private final MonitorInfo monitorInfo = new MonitorInfo();

    private final PlantDescriptionEntryMap entryMap;
    private final boolean secure;

    /**
     * Class constructor.
     *
     * @param entryMap An object that maps ID:s to Plant Description
     *                 Entries.
     * @param arSystem An Arrowhead Framework system used to provide this
     *                 service.
     * @param httpClient  Object for communicating with monitorable services.
     * @param insecure Indicates whether the service should run in secure mode.
     */
    public PdeMonitorService(ArSystem arSystem, PlantDescriptionEntryMap entryMap, HttpClient httpClient, boolean secure) {
        Objects.requireNonNull(arSystem, "Expected AR System");
        Objects.requireNonNull(entryMap, "Expected plant description map");
        Objects.requireNonNull(httpClient, "Expected HTTP client");

        this.arSystem = arSystem;
        this.entryMap = entryMap;
        this.secure = secure;

        this.monitorableClient = new MonitorableClient(arSystem, httpClient, monitorInfo);
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
            .name("plant-description-monitor")
            .encodings(EncodingDescriptor.JSON)
            .basePath("/pde/monitor")
            .get("/pd", new GetAllPlantDescriptions(monitorInfo, entryMap))
            .get("/alarm", new GetAllPdeAlarms());

        if (secure) {
            service.accessPolicy(AccessPolicy.cloud());
        } else {
            service.accessPolicy(AccessPolicy.unrestricted());
        }

        monitorableClient.start();
        return arSystem.provide(service);
    }

    /**
     * Determines whether or not this service is running in secure mode.
     */
    boolean isSecure() {
        return secure;
    }

}