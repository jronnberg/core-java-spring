package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.MonitorInfo;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.monitorable.dto.InventoryIdDto;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.monitorable.dto.SystemDataDto;
import se.arkalix.ArSystem;
import se.arkalix.description.ServiceDescription;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.descriptor.TransportDescriptor;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.query.ServiceQuery;

public class MonitorablesClient {

    private static final Logger logger = LoggerFactory.getLogger(MonitorablesClient.class);
    private final static int fetchInfoInterval = 6000; // Milliseconds
    private final static int pingInterval = 10000; // Milliseconds
    private ServiceQuery serviceQuery;
    private final HttpClient httpClient;
    private final MonitorInfo monitorInfo;
    private final AlarmManager alarmManager;

    /**
     * Constructor.
     *
     * @param arSystem     An Arrowhead Framework system used interact with the
     *                     orchestrator.
     * @param httpClient   Object for communicating with the other systems.
     * @param monitorInfo  Object used for keeping track of inventory data of
     *                     monitorable systems.
     * @param alarmManager Object used for managing PDE alarms.
     */
    MonitorablesClient(ArSystem arSystem, HttpClient httpClient, MonitorInfo monitorInfo, AlarmManager alarmManager) {
        Objects.requireNonNull(arSystem, "Expected AR System");
        Objects.requireNonNull(httpClient, "Expected HTTP client");
        Objects.requireNonNull(alarmManager, "Expected Alarm Manager");

        this.httpClient = httpClient;
        this.monitorInfo = monitorInfo;
        this.alarmManager = alarmManager;

        serviceQuery = arSystem.consume()
            .name("monitorable")
            .transports(TransportDescriptor.HTTP)
            .encodings(EncodingDescriptor.JSON);
    }

    public void start() {
        final var timer = new Timer();

        // Periodically check if all monitorable services are active
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ping();
            }
        }, 0, pingInterval);

        // Periodically request data from all monitorable services
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                retrieveMonitorInfo();
            }
        }, 0, fetchInfoInterval);
    }

    /**
     * Check if each monitorable service is active.
     */
    private void ping() {
        serviceQuery.resolveAll()
            .ifSuccess(services -> {
                for (var service : services) {
                    ping(service);
                }
            })
            .onFailure(e -> {
                logger.error("Failed to ping monitorable systems.", e);
            });
    }

    /**
     * Retrieve new data from each monitorable service.
     */
    private void retrieveMonitorInfo() {
        serviceQuery.resolveAll()
            .ifSuccess(services -> {
                for (var service : services) {
                    retrieveId(service);
                    retrieveSystemData(service);
                }
            })
            .onFailure(e -> {
                logger.error("Failed to fetch monitor info from monitorable systems.", e);
            });
    }

    private void ping(ServiceDescription service) {
        final var address = service.provider().socketAddress();
        final String providerName = service.provider().name();
        httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri(service.uri() + "/ping")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, InventoryIdDto.class))
            .ifSuccess(result -> {
                logger.info("Successfully pinged system '" + providerName + "'.");
                alarmManager.clearSystemInactive(providerName);
            })
            .onFailure(e -> {
                logger.warn("Failed to ping system '" + providerName + "'.", e);
                alarmManager.raiseSystemInactive(providerName);
            });
    }

    private void retrieveId(ServiceDescription service) {
        final var address = service.provider().socketAddress();

        httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri(service.uri() + "/inventoryid")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, InventoryIdDto.class))
            .ifSuccess(inventoryId -> {
                monitorInfo.putInventoryId(service, inventoryId.id());
            })
            .onFailure(e -> {
                String errorMessage = "Failed to retrieve inventory ID for system '" +
                    service.provider().name() + "', service '" + service.name() + "'.";
                logger.warn(errorMessage, e);

                // TODO: Raise an alarm?
            });
    }

    private void retrieveSystemData(ServiceDescription service) {
        final var address = service.provider().socketAddress();

        httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri(service.uri() + "/systemdata")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, SystemDataDto.class))
            .ifSuccess(systemData -> {
                monitorInfo.putSystemData(service, systemData.data());
            })
            .onFailure(e -> {
                String errorMessage = "Failed to retrieve system data for system '" +
                    service.provider().name() + "', service '" + service.name() + "'.";
                logger.error(errorMessage, e);
                // TODO: Raise an alarm?
            });
    }

}