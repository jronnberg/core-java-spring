package eu.arrowhead.core.plantdescriptionengine;

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
interface PlantDescriptionEntries {

    List<PlantDescriptionEntry> entries();

    default String asString() {
        String result = "[";
        List<String> strings = new ArrayList<>();
        for (var entry : entries()) {
            strings.add(entry.asString());
        }
        result += String.join(",", strings);
        result += "]";
        return result;
    }
}
