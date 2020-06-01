package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant description systems.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface PdeSystem {

    String systemId();
    Optional<String> systemName();
    Optional<Map<String, String>> metadata();
    List<Port> ports();
}