package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Optional;

/**
 * Data Transfer Object (DTO) interface for plant description system ports.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface Port {

    String portName();
    String serviceDefinition();

    /**
     * Indicates whether this port is used to consume or produce services.
     */
    Optional<Boolean> consumer();

    default String asString() {
        return "Port[portName=" + portName() + ",serviceDefinition=" + serviceDefinition() +
            ",consumer=" + consumer() + "]";
    }
}
