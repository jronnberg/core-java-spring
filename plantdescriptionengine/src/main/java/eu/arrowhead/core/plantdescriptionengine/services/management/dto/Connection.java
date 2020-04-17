package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for representing a plant description
 * connection, i.e. a connection between a service producer and a consumer.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface Connection {

    SystemPort consumer();
    SystemPort producer();

    default String asString() {
        return "Connection[consumer=" + consumer().asString() + ",producer=" + producer().asString() + "]";
    }
}
