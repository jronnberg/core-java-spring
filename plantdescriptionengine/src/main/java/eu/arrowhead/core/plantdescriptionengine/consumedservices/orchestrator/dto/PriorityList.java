package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import java.util.Map;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for priority list.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PriorityList {
    Map<String, Integer> priorityMap();

}
