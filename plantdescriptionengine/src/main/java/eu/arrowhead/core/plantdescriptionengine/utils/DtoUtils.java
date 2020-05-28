package eu.arrowhead.core.plantdescriptionengine.utils;

import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.ConnectionBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.ConnectionDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.MonitorInfo;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PortEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PortEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.SystemEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.SystemEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.SystemPortBuilder;

public final class DtoUtils {

    private DtoUtils() {}

    /**
     * Returns a copy of a Plant Description Entry supplemented with monitor
     * info.
     *
     * Note that the resulting copy will be an instance of
     * {@link eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PlantDescriptionEntryDto},
     * while the source entry is a
     * {@link eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry}
     * instance. This function is the glue between the mgmt and monitor
     * packages, letting data pass from one one to the other.
     *
     * @param entry The source entry on which the new one will be based.
     * @param monitorInfo Object used for keeping track of inventory data of
     *                    monitorable systems.
     * @return A PlantDescriptionEntry with all the information contained in the
     *         {@code entry} parameter, supplemented with any relevant info in
     *         the {@code monitorInfo} parameter.
     *
     */
    public static PlantDescriptionEntryDto extend(
        eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry entry,
        MonitorInfo monitorInfo
    ) {
        List<SystemEntryDto> systems = new ArrayList<>();
        List<ConnectionDto> connections = new ArrayList<>();

        for (var system : entry.systems()) {

            List<PortEntryDto> ports = new ArrayList<>();
            for (var port : system.ports()) {
                var portCopy = new PortEntryBuilder()
                    // TODO: Hiding the implementation of 'consumer' default
                    // values in here feels a bit iffy:
                    .portName(port.portName())
                    .serviceDefinition(port.serviceDefinition())
                    .consumer(port.consumer().orElse(false))
                    .metadata(port.metadata().orElse(null))
                    .build();
                ports.add(portCopy);
            }

            var systemBuilder = new SystemEntryBuilder()
                .systemId(system.systemId())
                .metadata(system.metadata().orElse(null))
                .ports(ports);

            if (system.systemName().isPresent()) {
                String systemName = system.systemName().get();
                systemBuilder.systemName(systemName);
                systemBuilder.inventoryId(monitorInfo.getInventoryId(systemName));
                systemBuilder.systemData(monitorInfo.getSystemData(systemName));
            }

            if (system.metadata().isPresent()) {
                systemBuilder.metadata(system.metadata().get());
            }

            systems.add(systemBuilder.build());
        }

        for (var connection : entry.connections()) {
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
            connections.add(connectionCopy);
        }

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