package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto;

import java.time.Instant;
import java.util.List;
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
    Optional<String> systemName();
    Optional<String> systemId();
    boolean acknowledged();
    String severity();
    String description();
    Instant raisedAt();
    Instant updatedAt();
    Optional<Instant> clearedAt();
    Optional<Instant> acknowledgedAt();

    /**
     * Filters out cleared/uncleared alarms from the given list.
     * @param alarms  A list of PDE alarms.
     * @param cleared If true, uncleared alarms are removed. If false, cleared
     *                alarms are removed.
     */
    static void filterCleared(List<? extends PdeAlarm> alarms, boolean cleared) {
        if (cleared) {
            alarms.removeIf(alarm -> alarm.clearedAt().isEmpty());
        } else {
            alarms.removeIf(alarm -> alarm.clearedAt().isPresent());
        }
    }

    /**
     * Filters out cleared/uncleared alarms from the given list.
     * @param alarms       A list of PDE alarms.
     * @param acknowledged If true, unacknowledged alarms are removed. If false,
     *                     acknowledged alarms are removed.
     */
    static void filterAcknowledged(List<? extends PdeAlarm> alarms, boolean acknowledged) {
        if (acknowledged) {
            alarms.removeIf(alarm -> alarm.acknowledged());
        } else {
            alarms.removeIf(alarm -> !alarm.acknowledged());
        }
    }

    /**
     * Filters the given list of alarms by system name.
     * @param alarms     A list of PDE alarms.
     * @param systemName Only alarms with this system name are kept in the list.
     */
	static void filterBySystemName(List<PdeAlarmDto> alarms, String systemName) {
        alarms.removeIf(alarm -> alarm.systemName().isEmpty() || !alarm.systemName().get().equals(systemName));
    }

    /**
     * Filters the given list of alarms based on their severity.
     * @param alarms   A list of PDE alarms.
     * @param severity Only alarms with this severity are kept in the list.
     */
	static void filterBySeverity(List<PdeAlarmDto> alarms, String severity) {
        alarms.removeIf(alarm -> !alarm.severity().equals(severity));

	}
}