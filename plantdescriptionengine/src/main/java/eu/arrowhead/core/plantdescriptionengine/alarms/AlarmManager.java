package eu.arrowhead.core.plantdescriptionengine.alarms;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AlarmManager {

    private final List<Alarm> activeAlarms = new ArrayList<>();
    private final List<Alarm> clearedAlarms = new ArrayList<>();

    /**
     * @param id The ID of a PDE Alarm.
     * @return Data describing the alarm with the given ID if it exists, null
     * otherwise.
     */
    private Alarm getAlarmData(int id) {
        for (final var alarm : activeAlarms) {
            if (alarm.id == id) {
                return alarm;
            }
        }
        return null;
    }

    /**
     * @return A list containing all PDE alarms.
     */
    public List<PdeAlarmDto> getAlarms() {
        final List<PdeAlarmDto> result = new ArrayList<>();

        final List<Alarm> allAlarms = new ArrayList<>();
        allAlarms.addAll(activeAlarms);
        allAlarms.addAll(clearedAlarms);

        for (final var alarm : allAlarms) {
            result.add(alarm.toPdeAlarm());
        }

        return result;
    }

    /**
     * @return A list containing the raw alarm data stored by this instance.
     */
    public List<Alarm> getActiveAlarmData(List<AlarmCause> causes) {
        return activeAlarms.stream().filter(alarm -> causes.contains(alarm.cause)).collect(Collectors.toList());
    }

    /**
     * @param id The ID of a PDE Alarm.
     * @return The PDE Alarm with the given ID if it exists, null otherwise.
     */
    public PdeAlarmDto getAlarmDto(int id) {
        final Alarm alarmData = getAlarmData(id);
        if (alarmData != null) {
            return alarmData.toPdeAlarm();
        }
        return null;
    }

    /**
     * @param id           ID of an alarm.
     * @param acknowledged The new value to assign to the alarm's acknowledged
     *                     field.
     */
    public void setAcknowledged(int id, boolean acknowledged) throws IllegalArgumentException {
        final Alarm alarm = getAlarmData(id);
        if (alarm == null) {
            throw new IllegalArgumentException("There is no alarm with ID " + id + ".");
        }
        alarm.acknowledged = acknowledged;
        alarm.acknowledgedAt = Instant.now();
    }

    private void raiseAlarm(String systemId, String systemName, AlarmCause cause) {
        // TODO: Concurrency handling

        // Check if this alarm has already been raised:
        for (final var alarm : activeAlarms) {
            if (alarm.matches(systemId, systemName, cause)) {
                return;
            }
        }
        activeAlarms.add(new Alarm(systemId, systemName, cause));
    }

    private void clearAlarm(String systemId, String systemName, AlarmCause cause) {
        final List<Alarm> newlyCleared = activeAlarms.stream()
            .filter(alarm -> alarm.matches(systemId, systemName, cause)).collect(Collectors.toList());

        for (var alarm : newlyCleared) {
            alarm.clearedAt = Instant.now();
            alarm.updatedAt = Instant.now();
        }

        clearedAlarms.addAll(newlyCleared);
        activeAlarms.removeAll(newlyCleared);
    }

    public void clearAlarm(Alarm alarm) {
        clearAlarm(alarm.systemId, alarm.systemName, alarm.cause);
    }

    public void raiseSystemNotRegistered(String systemId, String systemName) {
        Objects.requireNonNull(systemId, "Expected System ID");
        raiseAlarm(systemId, systemName, AlarmCause.systemNotRegistered);
    }

    public void raiseSystemInactive(String systemName) {
        raiseAlarm(null, systemName, AlarmCause.systemInactive);
    }

    public void clearSystemInactive(String systemName) {
        clearAlarm(null, systemName, AlarmCause.systemInactive);
    }

    public void raiseSystemNotInDescription(String systemName) {
        raiseAlarm(null, systemName, AlarmCause.systemNotInDescription);
    }

    public void clearSystemNotInDescription(String systemName) {
        clearAlarm(null, systemName, AlarmCause.systemNotInDescription);
    }

}