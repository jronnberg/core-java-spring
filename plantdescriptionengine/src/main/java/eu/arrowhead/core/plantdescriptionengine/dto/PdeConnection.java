package eu.arrowhead.core.plantdescriptionengine.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for representing a plant description
 * connection, i.e. a connection between a service producer and a consumer.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PdeConnection {

    PdeConnectionEndpoint consumer();
    PdeConnectionEndpoint producer();

    default String asString() {
        return "PdeConnection[consumer=" + consumer().asString() + ",producer=" + producer().asString() + "]";
    }
}
