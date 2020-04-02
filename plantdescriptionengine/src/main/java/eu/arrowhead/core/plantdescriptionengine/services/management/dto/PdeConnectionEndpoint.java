package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for representing one side (the consumer
 * or producer) of a plant description connection.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PdeConnectionEndpoint {

    String systemName();
    String portName();

    default String asString() {
        return "PdeConnectionEndpoint[systemName=" + systemName() + ",portName=" + portName() + "]";
    }
}
