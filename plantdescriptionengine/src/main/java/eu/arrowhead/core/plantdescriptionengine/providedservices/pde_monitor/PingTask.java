package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor;

import eu.arrowhead.core.plantdescriptionengine.ApiConstants;
import eu.arrowhead.core.plantdescriptionengine.alarms.Alarm;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmCause;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.Connection;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystem;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.Port;
import eu.arrowhead.core.plantdescriptionengine.utils.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ServiceRecord;
import se.arkalix.SystemRecord;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.query.ServiceQuery;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimerTask;

public class PingTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(PingTask.class);

    private final ServiceQuery serviceQuery;
    private final HttpClient httpClient;
    private final AlarmManager alarmManager;
    private final PlantDescriptionTracker pdTracker;

    public PingTask(
        final ServiceQuery serviceQuery,
        final HttpClient httpClient,
        final AlarmManager alarmManager,
        final PlantDescriptionTracker pdTracker
    ) {

        Objects.requireNonNull(serviceQuery, "Expected service query");
        Objects.requireNonNull(httpClient, "Expected HTTP client");
        Objects.requireNonNull(alarmManager, "Expected alarm manager");
        Objects.requireNonNull(pdTracker, "Expected Plant Description tracker");

        this.serviceQuery = serviceQuery;
        this.httpClient = httpClient;
        this.alarmManager = alarmManager;
        this.pdTracker = pdTracker;
    }

    @Override
    public void run() {

        serviceQuery.resolveAll()
            .ifSuccess(services -> {
                updateAlarmsForMissingServices(services);
                services.forEach(this::ping);
            })
            .onFailure(e -> logger.error("Failed to ping monitorable systems.", e));
    }

    private void updateAlarmsForMissingServices(Set<ServiceRecord> detectedMonitorableServices) {

        // TODO: Maybe this should be done per expected monitorable service
        // (i.e. per port with service definition 'monitorable'), not per
        // system?
        List<PdeSystem> monitoredSystems = getMonitoredSystems();
        final List<Alarm> previouslyDetectedAlarms = alarmManager.getActiveAlarmData(AlarmCause.NOT_MONITORABLE); // TODO: List of system's instead of list of alarms.
        final List<Alarm> currentlyDetectedAlarms = new ArrayList<>();

        for (PdeSystem system : monitoredSystems) {

            boolean hasMonitorableService = detectedMonitorableServices.stream()
                .anyMatch(service -> isProvidedBy(service.provider(), system));

            if (!hasMonitorableService) {
                final Alarm alarm = alarmManager.raiseSystemNotMonitorable(
                    system.systemId(),
                    system.systemName().orElse(null),
                    system.metadata()
                );
                currentlyDetectedAlarms.add(alarm);
            }
        }

        // TODO: The method below should probably be moved to AlarmManager
        // and reused for other alarm types.
        clearAlarmsNoLongerDetected(previouslyDetectedAlarms, currentlyDetectedAlarms);
    }

    private void clearAlarmsNoLongerDetected(
        final List<Alarm> previouslyDetectedAlarms,
        final List<Alarm> currentlyDetectedAlarms
    ) {

        for (final Alarm previouslyDetectedAlarm : previouslyDetectedAlarms) {
            if (currentlyDetectedAlarms.stream().noneMatch(alarm -> alarm.matches(previouslyDetectedAlarm))) {
                alarmManager.clearAlarm(previouslyDetectedAlarm);
            }
        }
    }

    private boolean isProvidedBy(SystemRecord provider, PdeSystem system) {
        boolean metadataMatch = Metadata.isSubset(system.metadata(), provider.metadata());
        boolean nameMatch = system.systemName().isEmpty() || system.systemName().get().equals(provider.name());
        return metadataMatch && nameMatch;
    }

    private List<PdeSystem> getMonitoredSystems() {
        final List<PdeSystem> result = new ArrayList<>();
        final PlantDescriptionEntry activeEntry = pdTracker.activeEntry();

        if (activeEntry == null) {
            return Collections.emptyList();
        }

        final List<PdeSystem> activeSystems = pdTracker.getActiveSystems();
        final Map<String, Port> monitorablePortBySystemId = new HashMap<>();
        final Map<String, PdeSystem> systemById = new HashMap<>();

        for (final PdeSystem system : activeSystems) {
            systemById.put(system.systemId(), system);
            for (final Port port : system.ports()) {
                if (port.serviceDefinition().equals(ApiConstants.MONITORABLE_SERVICE_NAME)) {
                    monitorablePortBySystemId.put(system.systemId(), port);
                }
            }
        }

        List<Connection> activeConnections = activeEntry.connections();
        for (final Connection connection : activeConnections) {

            final String producerId = connection.producer().systemId();
            final Port producerPort = monitorablePortBySystemId.get(producerId);

            if (producerPort == null) continue; // TODO: Check this logic.

            final String consumerId = connection.consumer().systemId();
            final PdeSystem consumer = systemById.get(consumerId);
            final String consumerName = consumer.systemName().orElse(null);

            boolean consumerIsPde = ApiConstants.PDE_SYSTEM_NAME.equals(consumerName); // TODO: Lowercase?
            boolean isMonitorableService = producerPort.serviceDefinition()
                .equals(ApiConstants.MONITORABLE_SERVICE_NAME);

            if (consumerIsPde && isMonitorableService) {
                result.add(systemById.get(producerId));
            }
        }

        return result;
    }

    private void ping(final ServiceRecord service) {
        final InetSocketAddress address = service.provider().socketAddress();
        final String providerName = service.provider().name();

        httpClient
            .send(address,
                new HttpClientRequest()
                    .method(HttpMethod.GET)
                    .uri(service.uri() + ApiConstants.MONITORABLE_PING_PATH)
                    .header(ApiConstants.HEADER_ACCEPT, ApiConstants.APPLICATION_JSON))
            .ifSuccess(result -> {
                logger.info("Successfully pinged system '" + providerName + "'.");
                alarmManager.clearNoPingResponse(providerName);
            })
            .onFailure(e -> {
                logger.warn("Failed to ping system '" + providerName + "'.", e);
                alarmManager.raiseNoPingResponse(providerName);
            });
    }

}
