package eu.arrowhead.core.plantdescriptionengine;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for representing one side (the consumer
 * or producer) of a plant description connection.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
interface PdeConnectionEndPoint {

    String systemName();
    String portName();

    default String asString() {
        return "PdeConnectionEndpoint[systemName=" + systemName() + ",portName=" + portName() + "]";
    }
}
