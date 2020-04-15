package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.util.List;
import java.util.Map;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant description system ports.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PdeSystem {

    String systemName();
    List<PdePort> ports();
    Map<String, String> metadata();

    default String asString() {
        return "PdeSystem[systemName=" + systemName() + "]";
    }
}
