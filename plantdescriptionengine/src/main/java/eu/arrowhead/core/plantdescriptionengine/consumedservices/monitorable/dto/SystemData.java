package eu.arrowhead.core.plantdescriptionengine.consumedservices.monitorable.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.value.JsonObject;

/**
 * Data Transfer Object (DTO) interface for System data.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface SystemData {
    JsonObject data();
}
