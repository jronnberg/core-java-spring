package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.services.monitorable.dto.InventoryIdDto;
import eu.arrowhead.core.plantdescriptionengine.services.monitorable.dto.SystemDataDto;
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
    private final static int pollForInfo = 3000; // Milliseconds
    private final static int infoPollInterval = 6000; // Milliseconds
    private ServiceQuery serviceQuery;
    private final HttpClient httpClient;
    private final MonitorInfo monitorInfo;

    MonitorablesClient(ArSystem arSystem, HttpClient httpClient, MonitorInfo monitorInfo) {
        Objects.requireNonNull(arSystem, "Expected AR System");
        Objects.requireNonNull(httpClient, "Expected HTTP client");
        Objects.requireNonNull(monitorInfo, "Expected MonitorInfo");

        this.httpClient = httpClient;
        this.monitorInfo = monitorInfo;

        serviceQuery = arSystem.consume()
            .name("monitorable")
            .transports(TransportDescriptor.HTTP)
            .encodings(EncodingDescriptor.JSON);
    }

    public void start() {
        Timer timer = new Timer();
        /*
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                pingPoll();
            }
        }, 0, pollForInfo);
        */

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                infoPoll();
            }
        }, 0, infoPollInterval);
    }

    /*
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
    */

    private void infoPoll() {
        serviceQuery.resolveAll()
            .ifSuccess(services -> {
                System.out.println("Number of services: " + services.size());
                for (var service : services) {
                    System.out.println(service.provider().name());
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
        httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/monitorable/ping")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, InventoryIdDto.class))
            .ifSuccess(pingData -> {
                System.out.println("Got ping from " + service.name());
            })
            .onFailure(e -> {
                System.out.println("No ping from " + service.name());
                // TODO: Raise an alarm
            });
    }

    private void retrieveId(ServiceDescription service) {
        final var address = service.provider().socketAddress();

        httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/monitorable/inventoryid")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, InventoryIdDto.class))
            .ifSuccess(inventoryId -> {
                monitorInfo.putInventoryId(service, inventoryId.id());
            })
            .onFailure(e -> {
                // TODO: Error handling, raise an alarm?
            });
    }

    private void retrieveSystemData(ServiceDescription service) {
        final var address = service.provider().socketAddress();

        httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/monitorable/systemdata")
            .header("accept", "application/json"))
            .flatMap(result -> result
                .bodyAsClassIfSuccess(DtoEncoding.JSON, SystemDataDto.class))
            .ifSuccess(systemData -> {
                monitorInfo.putSystemData(service, systemData.data());
            })
            .onFailure(e -> {
                // TODO: Error handling, raise an alarm?
            });
    }

}