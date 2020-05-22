package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.MonitorInfo;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface PlantDescriptionEntry {

    final static Comparator<PlantDescriptionEntry> ID_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(PlantDescriptionEntry e1, PlantDescriptionEntry e2) {
            return e1.id() - e2.id();
        }
    };

    final static Comparator<PlantDescriptionEntry> CREATED_AT_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(PlantDescriptionEntry e1, PlantDescriptionEntry e2) {
            return e1.createdAt().compareTo(e2.createdAt());
        }
    };

    final static Comparator<PlantDescriptionEntry> UPDATED_AT_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(PlantDescriptionEntry e1, PlantDescriptionEntry e2) {
            return e1.updatedAt().compareTo(e2.updatedAt());
        }
    };

    int id();
    String plantDescription();
    boolean active();
    List<Integer> include();
    List<PdeSystem> systems();
    List<Connection> connections();
    Instant createdAt();
    Instant updatedAt();

    /**
     * @param description The plant description to base this entry on.
     * @param id Identifier to be used for the new entry.
     * @return A new plant entry based on the given description.
     */
    static PlantDescriptionEntryDto from(PlantDescriptionDto description, int id) {
        List<PdeSystemDto> systems = new ArrayList<>();
        List<ConnectionDto> connections = new ArrayList<>();

        for (PdeSystem system : description.systems()) {
            systems.add((PdeSystemDto)system);
        }

        for (Connection connection : description.connections()) {
            connections.add((ConnectionDto)connection);
        }

        final Instant now = Instant.now();

        return new PlantDescriptionEntryBuilder()
            .id(id)
            .plantDescription(description.plantDescription())
            .active(description.active().orElse(false))
            .include(description.include().orElse(null))
            .systems(systems)
            .connections(connections)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    /**
     * @param oldEntry Target plant description entry to update.
     * @param newFields A plant description update.
     * @return A copy of the target plant description updated with the fields
     *         specified in newFields.
     */
    static PlantDescriptionEntryDto update(PlantDescriptionEntryDto oldEntry, PlantDescriptionUpdateDto newFields) {

        var builder = new PlantDescriptionEntryBuilder()
            .id(oldEntry.id())
            .plantDescription(newFields.plantDescription().orElse(oldEntry.plantDescription()))
            .active(newFields.active().orElse(oldEntry.active()))
            .include(newFields.include().orElse(oldEntry.include()))
            .createdAt(oldEntry.createdAt())
            .updatedAt(Instant.now());

        // The methods 'systems' and 'connections' return instances of
        // PdeSystem and Connection. This must be cast to their runtime-types
        // before they can be added to the old entry:
        List<PdeSystemDto> systems = new ArrayList<>();
        for (PdeSystem system : newFields.systems().orElse(oldEntry.systems())) {
            systems.add((PdeSystemDto)system);
        }
        builder.systems(systems);

        List<ConnectionDto> connections = new ArrayList<>();
        for (Connection connection : newFields.connections().orElse(oldEntry.connections())) {
            connections.add((ConnectionDto)connection);
        }
        builder.connections(connections);

        return builder.build();
    }

    static void sort(List<? extends PlantDescriptionEntry> entries, String sortField, boolean sortAscending) {

        Comparator<PlantDescriptionEntry> comparator = null;
        switch (sortField) {
            case "id":
                comparator = ID_COMPARATOR;
                break;
            case "createdAt":
                comparator = CREATED_AT_COMPARATOR;
                break;
            case "updatedAt":
                comparator = UPDATED_AT_COMPARATOR;
                break;
            default:
                assert false : sortField + " is not a valid sort field for Plant Description Entries.";
        }

        if (sortAscending) {
            Collections.sort(entries, comparator);
        } else {
            Collections.sort(entries, comparator.reversed());
        }
    }

    static void filterOnActive(
        List<? extends PlantDescriptionEntry> entries, boolean active
    ) {
        if (active) {
            entries.removeIf(entry -> !entry.active());
        } else {
            entries.removeIf(entry -> entry.active());
        }
    }

    /**
     * Helper function for finding service definition names.
     *
     * @param connectionIndex Index of a connection within this instance's
     *                        connection list.
     * @return Service definition name corresponding to the specified
     *         connection.
     */
    public default String serviceDefinitionName(int connectionIndex) {
        final SystemPort producerPort = connections().get(connectionIndex).producer();

        String serviceDefinitionName = null;

        for (PdeSystem system : systems()) {
            if (!system.systemId().equals(producerPort.systemId())) {
                continue;
            }
            for (Port port : system.ports()) {
                if (port.portName().equals(producerPort.portName())) {
                    serviceDefinitionName = port.serviceDefinition();
                }
            }
        }

        // TODO: Remove this and instead validate all plant descriptions.
        Objects.requireNonNull(serviceDefinitionName, "Could not find producer serviceDefinitionName in Plant Description Entry");
        return serviceDefinitionName;
    }

    default boolean matchesDescription(PlantDescription description) {
        return (
            description.plantDescription().equals(plantDescription()) &&
            (description.active().orElse(false) == active())
            // TODO: Check 'include', 'systems' and 'connections' as well.
        );
    }

    /**
     * Returns a copy of a Plant Description Entry supplemented with monitor
     * info.
     *
     * The resulting copy will contain inventory ID and system data, if
     * available.
     */
    // TODO: Remove this function
    static PlantDescriptionEntryDto extend(PlantDescriptionEntry entry, MonitorInfo monitorInfo) {
        List<PdeSystemDto> systems = new ArrayList<>();
        List<ConnectionDto> connections = new ArrayList<>();

        for (PdeSystem system : entry.systems()) {

            List<PortDto> ports = new ArrayList<>();
            for (Port port : system.ports()) {
                ports.add((PortDto)port);
            }

            var systemBuilder = new PdeSystemBuilder()
                .systemId(system.systemId())
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

        for (Connection connection : entry.connections()) {
            connections.add((ConnectionDto)connection);
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