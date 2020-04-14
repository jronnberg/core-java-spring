package eu.arrowhead.core.plantdescriptionengine.services.monitor.dto;

import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for PDE alarms updates.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PdeAlarmUpdate {

    Optional<Boolean> acknowledged();

    default String asString() {
        return "PdeAlarmUpdate[acknowledged=" + acknowledged() + "]";
    }

}