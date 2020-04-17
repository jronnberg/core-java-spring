package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant description systems.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PdeSystem {

    String systemName();
    Optional<Map<String, String>> metadata();
    List<Port> ports();

    default String asString() {
        return "PdeSystem[systemName=" + systemName() + "]";
    }
}
