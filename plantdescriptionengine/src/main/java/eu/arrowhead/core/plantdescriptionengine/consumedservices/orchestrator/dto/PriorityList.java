package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Map;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for priority list.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PriorityList {
    Map<String, Integer> priorityMap();

}
