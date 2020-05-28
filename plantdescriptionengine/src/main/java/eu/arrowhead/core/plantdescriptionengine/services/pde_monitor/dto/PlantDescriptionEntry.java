package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
    List<SystemEntry> systems();
    List<Connection> connections();
    Instant createdAt();
    Instant updatedAt();

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

}