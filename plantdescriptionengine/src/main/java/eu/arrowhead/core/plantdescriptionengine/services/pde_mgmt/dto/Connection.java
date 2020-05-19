package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for representing a plant description
 * connection, i.e. a connection between a service producer and a consumer.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface Connection {

    SystemPort consumer();
    SystemPort producer();
}
