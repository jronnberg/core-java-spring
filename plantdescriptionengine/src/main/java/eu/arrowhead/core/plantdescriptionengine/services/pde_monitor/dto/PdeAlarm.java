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
     * @param cleared If true, cleared alarms are removed. If false, uncleared
     *                alarms are removed.
     */
    static void filterByCleared(List<? extends PdeAlarm> alarms, boolean cleared) {
        if (cleared) {
            alarms.removeIf(alarm -> alarm.clearedAt().isEmpty());
        } else {
            alarms.removeIf(alarm -> alarm.clearedAt().isPresent());
        }
    }

    /**
     * Filters the given list of alarms by system name.
     * @param alarms     A list of PDE alarms.
     * @param systemNAme Only alarms with this system name are kept in the list.
     */
	static void filterBySystemName(List<PdeAlarmDto> alarms, String systemName) {
        alarms.removeIf(alarm -> alarm.systemName().isEmpty() || !alarm.systemName().get().equals(systemName)
        );
	}
}