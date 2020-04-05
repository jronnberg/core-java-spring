package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.util.ArrayList;
import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for lists of plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PlantDescriptionEntryList {

    int count();
    List<PlantDescriptionEntry> data();

    // TODO: Add functions for sorting and filtering

    // TODO: Check if data() is a copy or reference

    default String asString() {
        String result = "[";
        List<String> strings = new ArrayList<>();
        for (var entry : data()) {
            strings.add(entry.asString());
        }
        result += String.join(",", strings);
        result += "]";
        return result;
    }
}
