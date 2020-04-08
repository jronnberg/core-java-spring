package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    default String asString() {
        return "PlantDescriptionEntry[id=" + id() + ",plantDescription=" + plantDescription() + "]";
    }

}