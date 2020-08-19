package eu.arrowhead.core.plantdescriptionengine.alarmmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarm;

public class AlarmManagerTest {

    @Test
    public void shouldRaiseAlarmById() {
        final String systemId = "system_a";
        final var alarmManager = new AlarmManager();
        alarmManager.raiseAlarmBySystemId(systemId, AlarmManager.Cause.systemInactive);
        final PdeAlarm alarm = alarmManager.getAlarms().get(0);

        assertEquals(systemId, alarm.systemId().get());
        assertFalse(alarm.systemName().isPresent());
        assertFalse(alarm.acknowledged());
    }

    @Test
    public void shouldNotRaiseDuplicateAlarms() {
        final String systemName = "System A";
        final AlarmManager.Cause cause = AlarmManager.Cause.systemNotRegistered;
        final var alarmManager = new AlarmManager();

        alarmManager.raiseAlarmBySystemName(systemName, cause);
        alarmManager.raiseAlarmBySystemName(systemName, cause);

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertEquals(systemName, alarm.systemName().get());
        assertFalse(alarm.systemId().isPresent());
    }

    @Test
    public void shouldClearAlarmBySystemName() {
        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final AlarmManager.Cause cause = AlarmManager.Cause.systemNotRegistered;
        final var alarmManager = new AlarmManager();

        alarmManager.raiseAlarmBySystemName(systemNameA, cause);
        alarmManager.raiseAlarmBySystemName(systemNameB, cause);

        assertEquals(2, alarmManager.getAlarms().size());
        for (final var alarm : alarmManager.getAlarms()) {
            assertTrue(alarm.clearedAt().isEmpty());
        }

        alarmManager.clearAlarmBySystemName(systemNameA, cause);

        final var alarms = alarmManager.getAlarms();
        assertEquals(2, alarms.size());
        int numCleared = 0;
        for (final var alarm : alarmManager.getAlarms()) {
            if (alarm.clearedAt().isPresent()) {
                numCleared++;
                assertEquals(systemNameA, alarm.systemName().get());
            } else {
                assertEquals(systemNameB, alarm.systemName().get());
            }
        }
        assertEquals(1, numCleared);
    }

    @Test
    public void shouldClearAlarmBySystemId() {
        final String systemAId = "system_id";
        final String systemBName = "System B";
        final AlarmManager.Cause cause = AlarmManager.Cause.systemNotRegistered;
        final var alarmManager = new AlarmManager();

        alarmManager.raiseAlarmBySystemId(systemAId, cause);
        alarmManager.raiseAlarmBySystemName(systemBName, cause);

        assertEquals(2, alarmManager.getAlarms().size());

        alarmManager.clearAlarmBySystemId(systemAId, cause);

        final var alarms = alarmManager.getAlarms();
        assertEquals(2, alarms.size());
        int numCleared = 0;
        for (final var alarm : alarmManager.getAlarms()) {
            if (alarm.clearedAt().isPresent()) {
                numCleared++;
                assertEquals(systemAId, alarm.systemId().get());
            } else {
                assertEquals(systemBName, alarm.systemName().get());
            }
        }
        assertEquals(1, numCleared);
    }

}