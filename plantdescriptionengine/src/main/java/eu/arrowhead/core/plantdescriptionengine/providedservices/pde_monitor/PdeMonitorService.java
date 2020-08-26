package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor;

import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers.GetAllPdeAlarms;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers.GetPdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers.GetPlantDescription;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers.UpdatePdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers.GetAllPlantDescriptions;
import se.arkalix.ArServiceHandle;
import se.arkalix.ArSystem;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;

/**
 * This service enables monitoring of a plant and related alarms raised by Plant
 * Description Engine core system.
 */
public class PdeMonitorService {

    private final ArSystem arSystem;
    private final MonitorablesClient monitorableClient;
    private final MonitorInfo monitorInfo = new MonitorInfo();

    private final PlantDescriptionTracker pdTracker;
    private final boolean secure;

    /**
     * Class constructor.
     *
     * @param pdTracker An object that maps ID:s to Plant Description
     *                 Entries.
     * @param arSystem An Arrowhead Framework system used to provide this
     *                 service.
     * @param httpClient  Object for communicating with monitorable services.
     * @param insecure Indicates whether the service should run in secure mode.
     */
    public PdeMonitorService(
        ArSystem arSystem,
        PlantDescriptionTracker pdTracker,
        HttpClient httpClient,
        boolean secure
    ) {
        Objects.requireNonNull(arSystem, "Expected AR System");
        Objects.requireNonNull(pdTracker, "Expected plant description tracker");
        Objects.requireNonNull(httpClient, "Expected HTTP client");

        this.arSystem = arSystem;
        this.pdTracker = pdTracker;
        this.secure = secure;

        this.monitorableClient = new MonitorablesClient(arSystem, httpClient, monitorInfo);
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
        final var service = new HttpService()
            .name("plant-description-monitor")
            .encodings(EncodingDescriptor.JSON)
            .basePath("/pde/monitor")
            .get("/pd", new GetAllPlantDescriptions(monitorInfo, pdTracker))
            .get("/pd/#id", new GetPlantDescription(monitorInfo, pdTracker))
            .get("/alarm/#id", new GetPdeAlarm())
            .get("/alarm", new GetAllPdeAlarms())
            .patch("/alarm/#id", new UpdatePdeAlarm());

        if (secure) {
            service.accessPolicy(AccessPolicy.cloud());
        } else {
            service.accessPolicy(AccessPolicy.unrestricted());
        }

        monitorableClient.start();
        return arSystem.provide(service);
    }

}
