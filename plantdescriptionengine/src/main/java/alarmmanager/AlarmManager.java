package alarmmanager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmListBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmListDto;

public class AlarmManager {

    public enum Severity {
        indeterminate,
        critical,
        major,
        minor,
        warning,
        cleared
    }

    public enum Cause {
        systemInactive
    }

    private final List<PdeAlarmDto> alarms = new ArrayList<>();

    /**
     * @return A list containing all currently active alarms.
     */
	public PdeAlarmListDto getAlarmList() {
        return new PdeAlarmListBuilder()
            .count(alarms.size())
            .data(alarms)
            .build();
    }

    public void clearAlarm(String systemName, Cause cause) {
        // TODO: Implement
    }

    private static String getDescription(String systemName, Cause cause) {
        switch (cause) {
            case systemInactive:
                return "System '" + systemName + "' appears to be inactive.";
            default:
                throw new RuntimeException("Invalid alarm cause.");
        }
    }

	public void raiseAlarm(String systemName, Cause cause, Severity severity) {
        // TODO: Concurrency handling
        final Instant now = Instant.now();
        final PdeAlarmDto newAlarm = new PdeAlarmBuilder()
            .id(alarms.size())
            .systemName(systemName)
            .acknowledged(false)
            .severity(severity.toString())
            .description(getDescription(systemName, cause))
            .raisedAt(now)
            .updatedAt(now)
            .build();

        // If an alarm already exists for this issue, return immediately.
        for (final var existingAlarm : alarms) {
            if (PdeAlarm.sameIssue(newAlarm, existingAlarm)) {
                return;
            }
        }

        alarms.add(newAlarm);
    }

    public void raiseAlarm(Cause cause, Severity severity) {
        raiseAlarm("N/A", cause, severity);
	}

}