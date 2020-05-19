package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Map;
import java.util.Optional;

/**
 * Data Transfer Object (DTO) interface for plant description system ports.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface Port {

    String portName();
    String serviceDefinition();
    Optional<Map<String, String>> metadata();

    /**
     * Indicates whether this port is used to consume or produce services.
     */
    Optional<Boolean> consumer();
}
