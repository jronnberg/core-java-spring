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

    private List<PdeAlarmDto> alarms = new ArrayList<>();

    List<PdeAlarm> getAllAlarms() {
        return new ArrayList<>(alarms);
    }

	public PdeAlarmListDto getAlarmList() {
        return new PdeAlarmListBuilder()
            .count(alarms.size())
            .data(alarms)
            .build();
	}

	public void raiseAlarm(String systemName, String description, Severity severity) {
        final Instant now = Instant.now();
        alarms.add(new PdeAlarmBuilder()
            .systemName(systemName)
            .acknowledged(false)
            .severity(severity.toString())
            .description(description)
            .raisedAt(now)
            .updatedAt(now)
            .build());
    }

    public void raiseAlarm(String description, Severity severity) {
        raiseAlarm("N/A", description, severity);
	}

}