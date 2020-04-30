package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
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
            if (!system.systemName().equals(producerPort.systemName())) {
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

    default String asString() {
        return "PlantDescriptionEntry[id=" + id() + ",plantDescription=" + plantDescription() + "]";
    }

}