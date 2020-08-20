package eu.arrowhead.core.plantdescriptionengine.alarmmanager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmDto;

public class AlarmManager {

    // Integer for storing the next alarm ID to be used:
    private AtomicInteger nextId = new AtomicInteger();

    private static Map<Cause, Severity> severityByCause = Map.of(
        Cause.systemInactive, Severity.warning,
        Cause.systemNotRegistered, Severity.warning,
        Cause.systemNotInDescription, Severity.warning
    );

    private class AlarmData {

        /**
         * Internal representation of an alarm.
         */
        AlarmData(String systemId, String systemName, Cause cause) {
            this.id = nextId.getAndIncrement();
            this.systemId = systemId;
            this.systemName = systemName;
            this.cause = cause;
            this.acknowledged = false;

            raisedAt = Instant.now();
            updatedAt = Instant.now();
            clearedAt = null;
        }

        final int id;
        final String systemName;
        final String systemId;
        final Cause cause;
        boolean acknowledged;
        Instant raisedAt;
        Instant updatedAt;
        Instant clearedAt;
        Instant acknowledgedAt = null;

        private String description() {
            String identifier = (systemId == null)
                ? "named '" + systemName + "'"
                : "with ID '" + systemId + "'";

            switch (cause) {
                case systemInactive:
                    return "System " + identifier + " appears to be inactive.";
                case systemNotRegistered:
                    return "System " + identifier + " cannot be found in the Service Registry.";
                case systemNotInDescription:
                    return "System " + identifier + " is not present in the active Plant Description";
                default:
                    throw new RuntimeException("Invalid alarm cause.");
            }
        }

        private boolean matches(String systemId, String systemName, Cause cause) {
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

        /**
         * @return A PdeAlarm DTO based on this alarm data.
         */
        public PdeAlarmDto toPdeAlarm() {
            Severity severity = (clearedAt == null) ? severityByCause.get(cause) : Severity.cleared;
            return new PdeAlarmBuilder()
                .id(id)
                .systemId(systemId)
                .systemName(systemName)
                .acknowledged(acknowledged)
                .severity(severity.toString())
                .description(description())
                .raisedAt(raisedAt)
                .updatedAt(updatedAt)
                .clearedAt(clearedAt)
                .acknowledgedAt(acknowledgedAt)
                .build();
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
        systemInactive,
        systemNotRegistered,
        systemNotInDescription
    }

    static {
        for (final var cause : Cause.values()) {
            assert severityByCause.containsKey(cause) :
                "AlarmManager's severityByCause map not populated with all possible causes.";
        }
    }

    private final List<AlarmData> alarms = new ArrayList<>();
    private final List<AlarmData> clearedAlarms = new ArrayList<>();

    /**
     * @return A list containing all PDE alarms.
     */
	public List<PdeAlarmDto> getAlarms() {
        final List<PdeAlarmDto> result = new ArrayList<>();

        final List<AlarmData> allAlarms = new ArrayList<>();
        allAlarms.addAll(alarms);
        allAlarms.addAll(clearedAlarms);

        for (final var alarm : allAlarms) {
            result.add(alarm.toPdeAlarm());
        }

        return result;
    }

    /**
     * @param id The ID of a PDE Alarm.
     * @return The PDE Alarm with the given ID if it exists, null otherwise.
     */
    public PdeAlarmDto getAlarm(int id) {
		for (final var alarm : alarms) {
            if (alarm.id == id) {
                return alarm.toPdeAlarm();
            }
        }
        return null;
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

	public void raiseAlarmBySystemName(String systemName, Cause cause) {
        raiseAlarm(null, systemName, cause);
    }

    public void raiseAlarmBySystemId(String systemId, Cause cause) {
        raiseAlarm(systemId, null, cause);
    }

    private void clearAlarm(String systemId, String systemName, Cause cause) {
        final List<AlarmData> newlyCleared = alarms
            .stream()
            .filter(alarm -> alarm.matches(systemId, systemName, cause))
            .collect(Collectors.toList());

        for (var alarm : newlyCleared) {
            alarm.clearedAt = Instant.now();
            alarm.updatedAt = Instant.now();
        }

        clearedAlarms.addAll(newlyCleared);
        alarms.removeAll(newlyCleared);
    }

    public void clearAlarmBySystemName(String systemName, Cause cause) {
        clearAlarm(null, systemName, cause);
    }

    public void clearAlarmBySystemId(String systemId, Cause cause) {
        clearAlarm(systemId, null, cause);
    }

}