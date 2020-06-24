package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto;

import java.time.Instant;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for PDE alarms.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
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

    /**
     * @param a A PdeAlarm.
     * @param b A PdeAlarm.
     * @return True and only true if the two alarms refer to the same issue.
     */
	static boolean sameIssue(PdeAlarm a, PdeAlarm b) {
        return (
            a.systemName().equals(b.systemName()) &&
            a.severity() == b.severity() &&
            a.description().equals(b.description())
        );
	}

}