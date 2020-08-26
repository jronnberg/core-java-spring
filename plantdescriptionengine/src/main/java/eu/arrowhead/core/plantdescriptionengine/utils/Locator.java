package eu.arrowhead.core.plantdescriptionengine.utils;

import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.SystemTracker;

/**
 * Provides a global point of access to facilities that are fundamentally
 * singular in nature, e.g. the AlarmManager.
 *
 * The Singleton pattern is implemented using an Enum, as described in Joshua
 * Bloch's Effective Java.
 */
public enum Locator {

    INSTANCE; // Singleton instance.

    private AlarmManager alarmManager = null;
    private SystemTracker systemTracker = null;

    public static void setAlarmManager(AlarmManager alarmManager) {
        INSTANCE.alarmManager = alarmManager;
    }

    public static AlarmManager getAlarmManager() {
        if (INSTANCE.alarmManager == null) {
            throw new IllegalStateException("No alarm manager has been set.");
        }
        return INSTANCE.alarmManager;
    }

	public static void setSystemTracker(SystemTracker systemTracker) {
        INSTANCE.systemTracker = systemTracker;
    }

    public static SystemTracker getSystemTracker() {
        if (INSTANCE.systemTracker == null) {
            throw new IllegalStateException("No system tracker has been set.");
        }
        return INSTANCE.systemTracker;
    }


}