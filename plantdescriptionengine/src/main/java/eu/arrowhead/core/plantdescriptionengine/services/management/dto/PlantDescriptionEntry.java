package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PlantDescriptionEntry {

    int id();
    String plantDescription();
    boolean active();
    List<Integer> include(); // TODO: Check how this works with Optional
    List<PdeSystem> systems();
    List<PdeConnection> connections();
    Instant createdAt();
    Instant updatedAt();

    /**
     * @param description The plant description to base this entry on.
     * @param id Identifier to be used for the new entry.
     * @return A new plant entry based on the given description.
     */
    static PlantDescriptionEntryDto from(PlantDescriptionDto description, int id) {
        List<PdeSystemDto> systems = new ArrayList<>();
        List<PdeConnectionDto> connections = new ArrayList<>();

        for (PdeSystem system : description.systems()) {
            systems.add((PdeSystemDto)system);
        }

        for (PdeConnection connection : description.connections()) {
            connections.add((PdeConnectionDto)connection);
        }

        final Instant now = Instant.now();

        return new PlantDescriptionEntryBuilder()
            .id(id)
            .plantDescription(description.plantDescription())
            .active(description.active().orElse(false))
            .include(description.include())
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
        List<PdeSystemDto> systems = new ArrayList<>();
        List<PdeConnectionDto> connections = new ArrayList<>();

        for (PdeSystem system : newFields.systems()) {
            systems.add((PdeSystemDto)system);
        }

        for (PdeConnection connection : newFields.connections()) {
            connections.add((PdeConnectionDto)connection);
        }

        return new PlantDescriptionEntryBuilder()
            .id(oldEntry.id())
            .plantDescription(newFields.plantDescription().orElse(oldEntry.plantDescription()))
            .active(newFields.active().orElse(oldEntry.active()))
            .include(newFields.include())
            .systems(systems)
            .connections(connections)
            .createdAt(oldEntry.createdAt())
            .updatedAt(Instant.now())
            .build();
    }

    default String asString() {
        return "PlantDescriptionEntry[id=" + id() + ",plantDescription=" + plantDescription() + "]";
    }

}