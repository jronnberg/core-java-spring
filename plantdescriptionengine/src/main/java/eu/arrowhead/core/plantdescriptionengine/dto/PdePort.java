package eu.arrowhead.core.plantdescriptionengine.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant description system ports.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PdePort {

    String portName();
    String serviceDefinition();

    /**
     * Indicates whether this port is used to consume or produce services.
     */
    Boolean consumer();

    default String asString() {
        return "PdePort[portName=" + portName() + ",serviceDefinition=" + serviceDefinition() +
            ",consumer=" + consumer() + "]";
    }
}
