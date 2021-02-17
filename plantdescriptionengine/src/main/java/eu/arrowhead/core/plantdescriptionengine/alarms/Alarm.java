package eu.arrowhead.core.plantdescriptionengine.alarms;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmDto;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal representation of a PDE Alarm.
 */
public class Alarm {

    // Integer for storing the next alarm ID to be used:
    private final static AtomicInteger nextId = new AtomicInteger();

    Alarm(String systemId, String systemName, AlarmCause cause) {

        Objects.requireNonNull(cause, "Expected an alarm cause.");

        this.id = nextId.getAndIncrement();
        this.systemId = systemId;
        this.systemName = systemName;
        this.cause = cause;
        this.acknowledged = false;
        this.acknowledgedAt = null;

        raisedAt = Instant.now();
        updatedAt = Instant.now();
        clearedAt = null;
    }

    public final String systemName;
    public final String systemId;
    public final AlarmCause cause;
    final int id;
    final Instant raisedAt;
    boolean acknowledged;
    Instant updatedAt;
    Instant clearedAt;
    Instant acknowledgedAt;

    protected String description() {
        String identifier = (systemName == null)
            ? "with ID '" + systemId + "'"
            : "named '" + systemName + "'";

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
        return this.cause == cause;
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