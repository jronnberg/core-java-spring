package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto;

import java.time.Instant;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for PDE alarms.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PdeAlarm {

    int id();
    String systemName();
    boolean acknowledged();
    String severity();
    String description();
    Instant raisedAt();
    Instant updatedAt();
    Optional<Instant> clearedAt();
    Optional<Instant> acknowledgedAt();

    default String asString() {
        return "PdeAlarm[id=" + id() + ",description=" + description() + "]";
    }

}