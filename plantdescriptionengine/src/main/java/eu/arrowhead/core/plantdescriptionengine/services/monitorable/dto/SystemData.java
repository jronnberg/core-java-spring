package eu.arrowhead.core.plantdescriptionengine.services.monitorable.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Map;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for System data.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface SystemData {
	Map<String, String> data();
}
