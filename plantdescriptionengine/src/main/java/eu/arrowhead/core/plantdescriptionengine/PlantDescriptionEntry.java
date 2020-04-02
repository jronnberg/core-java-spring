package eu.arrowhead.core.plantdescriptionengine;

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
interface PlantDescriptionEntry {

    int id();
    String plantDescription();
    boolean active();
    List<Integer> include();
    List<PdeSystem> systems();
    List<PdeConnection> connections();
    // createdAt(); TODO: Add
    // updatedAt(); TODO: Add

    static PlantDescriptionEntryDto from(PlantDescriptionDto description) {
        List<PdeSystemDto> systems = new ArrayList<>();
        List<PdeConnectionDto> connections = new ArrayList<>();

        for (PdeSystem system : description.systems()) {
            systems.add((PdeSystemDto)system);
        }
        for (PdeConnection connection : description.connections()) {
            connections.add((PdeConnectionDto)connection);
        }

        return new PlantDescriptionEntryBuilder()
            .id(description.id())
            .plantDescription(description.plantDescription())
            .active(description.active())
            .include(description.include())
            .systems(systems)
            .connections(connections)
            .build();
    }

    default String asString() {
        return "PlantDescriptionEntry[id=" + id() + ",plantDescription=" + plantDescription() + ",active=" + active() + "]";
    }

}
