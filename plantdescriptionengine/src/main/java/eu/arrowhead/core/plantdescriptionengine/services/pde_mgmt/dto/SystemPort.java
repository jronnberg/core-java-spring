package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for representing one side (the consumer
 * or producer) of a plant description connection.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SystemPort {

    Integer systemId();
    String portName();

    default String asString() {
        return "SystemPortEndpoint[systemId=" + systemId() + ",portName=" + portName() + "]";
    }
}
