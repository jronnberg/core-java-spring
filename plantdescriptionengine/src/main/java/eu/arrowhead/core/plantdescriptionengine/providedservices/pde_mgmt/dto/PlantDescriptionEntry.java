package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface PlantDescriptionEntry {

    Comparator<PlantDescriptionEntry> ID_COMPARATOR = Comparator.comparingInt(PlantDescriptionEntry::id);

    Comparator<PlantDescriptionEntry> CREATED_AT_COMPARATOR = Comparator.comparing(PlantDescriptionEntry::createdAt);

    Comparator<PlantDescriptionEntry> UPDATED_AT_COMPARATOR = Comparator.comparing(PlantDescriptionEntry::updatedAt);

    /**
     * @param description The plant description to base this entry on.
     * @param id          Identifier to be used for the new entry.
     * @return A new plant entry based on the given description.
     */
    static PlantDescriptionEntryDto from(PlantDescriptionDto description, int id) {
        List<PdeSystemDto> systems = new ArrayList<>();
        List<ConnectionDto> connections = new ArrayList<>();

        for (PdeSystem system : description.systems()) {
            systems.add((PdeSystemDto) system);
        }

        for (Connection connection : description.connections()) {
            connections.add((ConnectionDto) connection);
        }

        final Instant now = Instant.now();

        return new PlantDescriptionEntryBuilder().id(id).plantDescription(description.plantDescription())
            .active(description.active().orElse(false)).include(description.include().orElse(null)).systems(systems)
            .connections(connections).createdAt(now).updatedAt(now).build();
    }

    /**
     * @param entry A Plant Description Entry.
     * @return A copy of the given entry, with 'active' set to false.
     */
    static PlantDescriptionEntryDto deactivated(PlantDescriptionEntry entry) {
        List<PdeSystemDto> systems = new ArrayList<>();
        List<ConnectionDto> connections = new ArrayList<>();

        for (PdeSystem system : entry.systems()) {
            systems.add((PdeSystemDto) system);
        }

        for (Connection connection : entry.connections()) {
            connections.add((ConnectionDto) connection);
        }

        return new PlantDescriptionEntryBuilder().id(entry.id()).plantDescription(entry.plantDescription())
            .active(false).include(entry.include()).systems(systems).connections(connections)
            .createdAt(entry.createdAt()).updatedAt(Instant.now()).build();
    }

    /**
     * @param oldEntry  Target plant description entry to update.
     * @param newFields A plant description update.
     * @return A copy of the target plant description updated with the fields
     * specified in newFields.
     */
    static PlantDescriptionEntryDto update(PlantDescriptionEntryDto oldEntry, PlantDescriptionUpdateDto newFields) {

        var builder = new PlantDescriptionEntryBuilder().id(oldEntry.id())
            .plantDescription(newFields.plantDescription().orElse(oldEntry.plantDescription()))
            .active(newFields.active().orElse(oldEntry.active()))
            .include(newFields.include().orElse(oldEntry.include())).createdAt(oldEntry.createdAt())
            .updatedAt(Instant.now());

        // The methods 'systems' and 'connections' return instances of
        // PdeSystem and Connection. This must be cast to their runtime-types
        // before they can be added to the old entry:
        List<PdeSystemDto> systems = new ArrayList<>();
        for (PdeSystem system : newFields.systems().orElse(oldEntry.systems())) {
            systems.add((PdeSystemDto) system);
        }
        builder.systems(systems);

        List<ConnectionDto> connections = new ArrayList<>();
        for (Connection connection : newFields.connections().orElse(oldEntry.connections())) {
            connections.add((ConnectionDto) connection);
        }
        builder.connections(connections);

        return builder.build();
    }

    static void sort(List<? extends PlantDescriptionEntry> entries, String sortField, boolean sortAscending) {

        Comparator<PlantDescriptionEntry> comparator;
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
                throw new IllegalArgumentException(
                    "'" + sortField + "' is not a valid sort field for Plant Description Entries.");
        }

        if (sortAscending) {
            entries.sort(comparator);
        } else {
            entries.sort(comparator.reversed());
        }
    }

    /**
     * Filters the given list based on the elements' 'active' values.
     *
     * @param entries A list of Plant Description entries.
     * @param active  If true, active entries are removed. If false, inactive
     *                entries are removed.
     */
    static void filterByActive(List<? extends PlantDescriptionEntry> entries, boolean active) {
        if (active) {
            entries.removeIf(entry -> !entry.active());
        } else {
            entries.removeIf(PlantDescriptionEntry::active);
        }
    }

    int id();

    String plantDescription();

    boolean active();

    List<Integer> include();

    List<PdeSystem> systems();

    List<Connection> connections();

    Instant createdAt();

    Instant updatedAt();

    /**
     * Helper function for finding service definition names.
     *
     * @param connectionIndex Index of a connection within this instance's
     *                        connection list.
     * @return Service definition name of the *producer* service of the specified
     * connection.
     */
    default String serviceDefinitionName(int connectionIndex) { // TODO: Remove
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
        Objects.requireNonNull(serviceDefinitionName, "Could not find producer of connection " + connectionIndex
            + " in Plant Description Entry '" + plantDescription() + "'");
        return serviceDefinitionName;
    }

    /**
     * @param description A Plant Description.
     * @return True if the fields of the given description match those of this
     * instance.
     */
    default boolean matchesDescription(PlantDescription description) {
        return (description.plantDescription().equals(plantDescription())
            && (description.active().orElse(false) == active())
            // TODO: Check 'include', 'systems' and 'connections' as well.
        );
    }

}