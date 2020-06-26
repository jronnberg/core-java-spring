package alarmmanager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmListBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmListDto;

public class AlarmManager {

    private Map<Cause, Severity> severityByCause = Map.of(
        Cause.systemInactive, Severity.warning
        // TODO: Add severities for all possible causes
    );

    private class AlarmData {

        /**
         * Internal representation of an alarm.
         */
        AlarmData(String systemId, String systemName, Cause cause) {
            this.systemId = systemId;
            this.systemName = systemName;
            this.cause = cause;
            this.acknowledged = false;

            raisedAt = Instant.now();
            updatedAt = Instant.now();
        }

        public final String systemName;
        public final String systemId;
        public final Cause cause;
        public boolean acknowledged;
        public Instant raisedAt;
        public Instant updatedAt;
        public Optional<Instant> acknowledgedAt = Optional.empty();

        private String description() {
            switch (cause) {
                case systemInactive:
                    return "System '" + systemName + "' appears to be inactive.";
                default:
                    throw new RuntimeException("Invalid alarm cause.");
            }
        }

        public boolean matches(String systemId, String systemName, Cause cause) {
            if (systemName == null && systemId == null) {
                return false;
            }
            if (systemName != null && !systemName.equals(this.systemName)) {
                return false;
            }
            if (systemId != null && !systemId.equals(this.systemId)) {
                return false;
            }
            if (this.cause != cause) {
                return false;
            }
            return true;
        }
    }

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

    private final List<AlarmData> alarms = new ArrayList<>();

    /**
     * @return A list containing all currently active alarms.
     */
	public PdeAlarmListDto getAlarmList() {
        final var alarmDtos = new ArrayList<PdeAlarmDto>();

        for (final var alarm : alarms) {
            Severity severity = severityByCause.get(alarm.cause);
            final var alarmBuilder = new PdeAlarmBuilder()
                .id(alarms.size())
                .systemName(alarm.systemName)
                .acknowledged(alarm.acknowledged)
                .severity(severity.toString())
                .description(alarm.description())
                .raisedAt(alarm.raisedAt)
                .updatedAt(alarm.updatedAt);
            if (alarm.acknowledgedAt.isPresent()) {
                alarmBuilder.acknowledgedAt(alarm.acknowledgedAt.get());
            }
            alarmDtos.add(alarmBuilder.build());
        }

        return new PdeAlarmListBuilder()
            .count(alarmDtos.size())
            .data(alarmDtos)
            .build();
    }

    private void raiseAlarm(String systemId, String systemName, Cause cause) {
        // TODO: Concurrency handling

        // Check if this alarm has already been raised:
        for (final var alarm : alarms) {
            if (alarm.matches(systemId, systemName, cause)) {
                return;
            }
        }
        alarms.add(new AlarmData(systemId, systemName, cause));
    }

	public void raiseAlarmBySystemByName(String systemName, Cause cause) {
        raiseAlarm(null, systemName, cause);
    }

    public void raiseAlarmForSystemById(String systemId, Cause cause) {
        raiseAlarm(systemId, null, cause);
    }

    private void clearAlarm(String systemId, String systemName, Cause cause) {
        final var clearedAlarms = alarms
            .stream()
            .filter(alarm -> alarm.matches(systemId, systemName, cause))
            .collect(Collectors.toList());
        alarms.removeAll(clearedAlarms);
    }

    public void clearAlarmBySystemName(String systemName, Cause cause) {
        clearAlarm(null, systemName, cause);
    }

    public void clearAlarmBySystemId(String systemId, Cause cause) {
        clearAlarm(systemId, null, cause);
    }

}