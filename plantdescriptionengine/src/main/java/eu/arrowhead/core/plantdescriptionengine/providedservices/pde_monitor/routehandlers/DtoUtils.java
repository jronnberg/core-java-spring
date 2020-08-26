package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.ConnectionBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.ConnectionDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.Connection;
import eu.arrowhead.core.plantdescriptionengine.MonitorInfo;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PortEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PortEntryDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.SystemEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.SystemEntryDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.SystemPortBuilder;

public final class DtoUtils {

    private DtoUtils() {}

    /**
     * Converts the provided list of {@link
     * eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.Connection}
     * to a list of {@link}
     * eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.Connection}
     * objects.
     *
     * @param connections A list of connections adhering to the mgmt package
     *                   format.
     * @return A list of connections adhering to the monitor package format.
     */
    private static List<ConnectionDto> mgmtToMonitor(List<Connection> connections) {
        List<ConnectionDto> result = new ArrayList<>();

        for (var connection : connections) {
            var consumerPort = new SystemPortBuilder()
                .portName(connection.consumer().portName())
                .systemId(connection.consumer().systemId())
                .build();
            var producerPort = new SystemPortBuilder()
                .portName(connection.producer().portName())
                .systemId(connection.producer().systemId())
                .build();

            var connectionCopy = new ConnectionBuilder()
                .consumer(consumerPort)
                .producer(producerPort)
                .build();
            result.add(connectionCopy);
        }
        return result;
    }

    /**
     * Returns a Plant Description Entry supplemented with monitor info.
     *
     * Note that the resulting copy will be an instance of {@link
     * eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PlantDescriptionEntryDto},
     * while the source entry is a {@link
     * eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry}
     * instance. This function is the glue between the mgmt and monitor
     * packages, letting data flow from one one to the other.
     *
     * @param entry       The source entry on which the new one will be based.
     * @param monitorInfo Object used for keeping track of inventory data of
     *                    monitorable systems.
     * @return A PlantDescriptionEntry with all the information contained in the
     *         {@code entry} parameter, supplemented with any relevant info in
     *         the {@code monitorInfo} parameter.
     *
     */
    public static PlantDescriptionEntryDto extend(
            eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry entry,
            MonitorInfo monitorInfo) {
        List<SystemEntryDto> systems = new ArrayList<>();

        for (var system : entry.systems()) {

            List<MonitorInfo.Bundle> systemInfoList = monitorInfo.getSystemInfo(
                system.systemName().orElse(null),
                system.metadata().orElse(null)
            );

            List<PortEntryDto> ports = new ArrayList<>();
            for (var port : system.ports()) {

                // 'consumer' defaults to false when no value is set:
                boolean isConsumer = port.consumer().orElse(false);

                var portBuilder = new PortEntryBuilder()
                    .portName(port.portName())
                    .serviceDefinition(port.serviceDefinition())
                    .consumer(isConsumer)
                    .metadata(port.metadata().orElse(null));

                // Only add monitor info to ports where this system is the
                // provider:
                if (!isConsumer) {

                    MonitorInfo.Bundle serviceMonitorInfo = null;

                    for (var info : systemInfoList) {
                        boolean matchesServiceDefinition = info.serviceDefinition.equals(port.serviceDefinition());
                        boolean matchesPort = info.matchesPortMetadata(system.metadata(), port.metadata());

                        if (matchesServiceDefinition && matchesPort) {
                            serviceMonitorInfo = info;
                            systemInfoList.remove(info);
                            break;
                        }
                    }

                    if (serviceMonitorInfo != null) {
                        portBuilder.systemData(serviceMonitorInfo.systemData);
                        portBuilder.inventoryId(serviceMonitorInfo.inventoryId);
                    }
                }

                ports.add(portBuilder.build());
            }

            var systemBuilder = new SystemEntryBuilder()
                .systemId(system.systemId())
                .metadata(system.metadata().orElse(null))
                .ports(ports);

            // If there is any monitor info left, that means it has not been
            // matched to a specific service, and should possibly be presented
            // on the system entry itself.
            if (systemInfoList.size() > 0) {
                var infoBundle = systemInfoList.get(0);
                if (infoBundle.matchesSystemMetadata(system.metadata())) {
                    systemBuilder
                        .inventoryId(infoBundle.inventoryId)
                        .systemData(infoBundle.systemData);
                }
            }

            systems.add(systemBuilder.build());
        }

        List<ConnectionDto> connections = mgmtToMonitor(entry.connections());

        return new PlantDescriptionEntryBuilder()
            .id(entry.id())
            .plantDescription(entry.plantDescription())
            .active(entry.active())
            .include(entry.include())
            .systems(systems)
            .connections(connections)
            .createdAt(entry.createdAt())
            .updatedAt(entry.updatedAt())
            .build();
    }
}