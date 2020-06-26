package eu.arrowhead.core.plantdescriptionengine.utils;

import alarmmanager.AlarmManager;

/**
 * Singleton class for providing a global point of access to services.
 *
 * The Singleton pattern is implemented using an Enum, as described in Joshua
 * Bloch's Effective Java.
 */
public enum Locator {

    INSTANCE; // Singleton instance.

    private AlarmManager alarmManager = null;

    public static void setAlarmManager(AlarmManager alarmManager) {
        INSTANCE.alarmManager = alarmManager;
    }

    public static AlarmManager getAlarmManager() {
        if (INSTANCE.alarmManager == null) {
            throw new IllegalStateException("No alarm manager has been set.");
        }
        return INSTANCE.alarmManager;
    }

}