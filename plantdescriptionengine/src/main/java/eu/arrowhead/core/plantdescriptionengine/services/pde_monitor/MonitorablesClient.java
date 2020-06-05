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
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.query.ServiceQuery;

public class MonitorablesClient {

    private static final Logger logger = LoggerFactory.getLogger(MonitorablesClient.class);

    private final static int pollInterval = 5000; // Milliseconds
    private final ServiceQuery serviceQuery;
    private final HttpClient httpClient;

    private final MonitorInfo monitorInfo;

    MonitorablesClient(ArSystem arSystem, HttpClient httpClient, MonitorInfo monitorInfo) {
        Objects.requireNonNull(arSystem, "Expected AR System");
        Objects.requireNonNull(httpClient, "Expected HTTP client");
        Objects.requireNonNull(monitorInfo, "Expected MonitorInfo");

        serviceQuery = arSystem.consume()
            .name("monitorable")
            .encodings(EncodingDescriptor.JSON);
        this.httpClient = httpClient;
        this.monitorInfo = monitorInfo;
    }

    public void start() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                pollMonitorableSystems();
            }
        }, 0, pollInterval);
    }

    private void pollMonitorableSystems() {
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