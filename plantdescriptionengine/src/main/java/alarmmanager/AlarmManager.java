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
        AlarmData(String systemName, Cause cause) {
            this.systemName = systemName;
            this.cause = cause;
            this.acknowledged = false;

            raisedAt = Instant.now();
            updatedAt = Instant.now();
        }

        public final String systemName;
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

        public boolean matches(String systemName, Cause cause) {
            System.out.println("Checking for match");
            System.out.println(systemName + ", " + cause);
            System.out.println(this.systemName + ", " + this.cause);
            System.out.println(this.systemName.equals(systemName) && this.cause == cause);
            return this.systemName.equals(systemName) && this.cause == cause;
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

    public void clearAlarm(String systemName, Cause cause) {
        System.out.println("Clearing alarms:");
        final var clearedAlarms = alarms
            .stream()
            .filter(alarm -> alarm.matches(systemName, cause))
            .collect(Collectors.toList());
            System.out.println(clearedAlarms);
        alarms.removeAll(clearedAlarms);
    }

	public void raiseAlarm(String systemName, Cause cause) {
        // TODO: Concurrency handling

        // Check if this alarm has already been raised:
        for (final var alarm : alarms) {
            if (alarm.matches(systemName, cause)) {
                return;
            }
        }
        alarms.add(new AlarmData(systemName, cause));
    }

    public void raiseAlarm(Cause cause) {
        raiseAlarm("N/A", cause);
	}

}