package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto;

import java.util.ArrayList;
import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for lists of systems registered in service registry.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SrSystemList {

    List<SrSystem> data();
    int count();

    default String asString() {
        String result = "SrSystemList[count=" + count() + ",data=[";
        List<String> strings = new ArrayList<>();
        for (var entry : data()) {
            strings.add(String.valueOf(entry.id()));
        }
        result += String.join(",", strings);
        result += "]]";
        return result;
    }
}
