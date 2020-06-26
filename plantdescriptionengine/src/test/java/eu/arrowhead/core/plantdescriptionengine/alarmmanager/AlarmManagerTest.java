package eu.arrowhead.core.plantdescriptionengine.alarmmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmListDto;

public class AlarmManagerTest {

    @Test
    public void shouldRaiseAlarmById() {
        final String systemId = "system_a";
        final var alarmManager = new AlarmManager();
        alarmManager.raiseAlarmBySystemId(systemId, AlarmManager.Cause.systemInactive);
        final PdeAlarm alarm = alarmManager.getAlarmList().data().get(0);

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

        final PdeAlarmListDto alarms = alarmManager.getAlarmList();
        assertEquals(1, alarms.count());
        final var alarm = alarms.data().get(0);
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

        assertEquals(2, alarmManager.getAlarmList().count());

        alarmManager.clearAlarmBySystemName(systemNameA, cause);

        final PdeAlarmListDto alarms = alarmManager.getAlarmList();
        assertEquals(1, alarms.count());
        final var alarm = alarms.data().get(0);
        assertEquals(systemNameB, alarm.systemName().get());
        assertFalse(alarm.systemId().isPresent());
    }

    @Test
    public void shouldClearAlarmBySystemId() {
        final String systemId = "system_id";
        final String systemName = "System B";
        final AlarmManager.Cause cause = AlarmManager.Cause.systemNotRegistered;
        final var alarmManager = new AlarmManager();

        alarmManager.raiseAlarmBySystemId(systemId, cause);
        alarmManager.raiseAlarmBySystemName(systemName, cause);

        assertEquals(2, alarmManager.getAlarmList().count());

        alarmManager.clearAlarmBySystemId(systemId, cause);

        final PdeAlarmListDto alarms = alarmManager.getAlarmList();
        assertEquals(1, alarms.count());
        final var alarm = alarms.data().get(0);
        assertEquals(systemName, alarm.systemName().get());
        assertFalse(alarm.systemId().isPresent());
    }

}