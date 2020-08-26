package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.MonitorInfo;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.monitorable.dto.InventoryIdDto;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.monitorable.dto.SystemDataDto;
import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
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
    private final static int infoPollInterval = 6000; // Milliseconds
    private final static int pingPollInterval = 10000; // Milliseconds
    private ServiceQuery serviceQuery;
    private final HttpClient httpClient;
    private final MonitorInfo monitorInfo;

    MonitorablesClient(ArSystem arSystem, HttpClient httpClient, MonitorInfo monitorInfo) {
        Objects.requireNonNull(arSystem, "Expected AR System");
        Objects.requireNonNull(httpClient, "Expected HTTP client");

        this.httpClient = httpClient;
        this.monitorInfo = monitorInfo;

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
                pingPoll();
            }
        }, 0, pingPollInterval);

        // Periodically request data from all monitorable services
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                infoPoll();
            }
        }, 0, infoPollInterval);
    }

    /**
     * Check if each monitorable service is active.
     */
    private void pingPoll() {
        serviceQuery.resolveAll()
            .ifSuccess(services -> {
                for (var service : services) {
                    ping(service);
                }
            })
            .onFailure(e -> {
                logger.error("Failed to poll monitorable systems.", e);
            });
    }

    /**
     * Retrieve new data from each monitorable service.
     */
    private void infoPoll() {
        serviceQuery.resolveAll()
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
                System.out.println("Successful ping");
                Locator.getAlarmManager().clearSystemInactive(providerName);
            })
            .onFailure(e -> {
                System.out.println("Failed ping");
                Locator.getAlarmManager().raiseSystemInactive(providerName);
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
                e.printStackTrace();
                // TODO: Error handling, raise an alarm?
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
                e.printStackTrace();
                // TODO: Error handling, raise an alarm?
            });
    }

}