package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto;

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

    List<PlantDescriptionEntry> data();
    int count();

    default String asString() {
        String result = "PlantDescriptionEntryList[count=" + count() + ",data=[";
        List<String> strings = new ArrayList<>();
        for (var entry : data()) {
            strings.add(String.valueOf(entry.id()));
        }
        result += String.join(",", strings);
        result += "]]";
        return result;
    }
}