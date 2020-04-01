package eu.arrowhead.core.plantdescriptionengine;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for representing a plant description
 * connection, i.e. a connection between a service producer and a consumer.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
interface PdeConnection {

    PdeConnectionEndPoint consumer();
    PdeConnectionEndPoint producer();

    default String asString() {
        return "PdeConnection[consumer=" + consumer().asString() + ",producer=" + producer().asString() + "]";
    }
}
