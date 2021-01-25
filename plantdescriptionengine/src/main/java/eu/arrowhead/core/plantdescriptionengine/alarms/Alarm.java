package eu.arrowhead.core.plantdescriptionengine.alarms;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmDto;

/**
 * Internal representation of a PDE Alarm.
 */
public class Alarm {

    // Integer for storing the next alarm ID to be used:
    private static AtomicInteger nextId = new AtomicInteger();

    Alarm(String systemId, String systemName, AlarmCause cause) {

        Objects.requireNonNull(cause, "Expected an alarm cause.");

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
    public final String systemName;
    public final String systemId;
    public final AlarmCause cause;
    public final Map<String, String> metadata = null; // TODO: Make use of this.
    boolean acknowledged;
    Instant raisedAt;
    Instant updatedAt;
    Instant clearedAt;
    Instant acknowledgedAt = null;

    protected String description() {
        String identifier = (systemId == null)
            ? "named '" + systemName + "'"
            : "with ID '" + systemId + "'";

        switch (cause) {
            case systemInactive:
                return "System " + identifier + " appears to be inactive.";
            case systemNotRegistered:
                return "System " + identifier + " cannot be found in the Service Registry.";
            case systemNotInDescription:
                return "System " + identifier + " is not present in the active Plant Description.";
            default:
                throw new RuntimeException("Invalid alarm cause.");
        }
    }

    public boolean matches(String systemId, String systemName, AlarmCause cause) {

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
        AlarmSeverity severity = (clearedAt == null) ? AlarmSeverity.warning : AlarmSeverity.cleared;
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