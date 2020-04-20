package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for cloud.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface Cloud {
	String operator();
	String name();

    default String asString() {
        return "Cloud[operator=" + operator() + ", name="+ name() + "]";
    }
}
