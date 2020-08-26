package eu.arrowhead.core.plantdescriptionengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionUpdateListener;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystem;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.SystemUpdateListener;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystem;
import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import eu.arrowhead.core.plantdescriptionengine.alarmmanager.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.alarmmanager.AlarmManager.AlarmData;

public class PdMismatchDetector implements PlantDescriptionUpdateListener, SystemUpdateListener {
    private PlantDescriptionTracker pdTracker;

    public PdMismatchDetector(PlantDescriptionTracker pdTracker) {
        Objects.requireNonNull(pdTracker, "Expected Plant Description Tracker");
        this.pdTracker = pdTracker;
    }

    /**
     * Start monitoring Plant Descriptions and registered systems, raising
     * alarms whenever there is a mismatch, and clearing alarms when issues are
     * solved.
     */
    public void run() {
        pdTracker.addListener(this);
        Locator.getSystemTracker().addListener(this);

        // Initial check for mismatches:
        checkSystems();
    }

    @Override
    public void onPlantDescriptionAdded(PlantDescriptionEntry entry) {
        checkSystems();
    }

    @Override
    public void onPlantDescriptionUpdated(PlantDescriptionEntry entry) {
        checkSystems();
    }

    @Override
    public void onPlantDescriptionRemoved(PlantDescriptionEntry entry) {
        checkSystems();
    }

    @Override
    public void onSystemAdded(SrSystem system) {
        checkSystems();
    }

    @Override
    public void onSystemRemoved(SrSystem system) {
        checkSystems();
    }

    /**
     *
     * @param entrySystem A Plant Description Entry system.
     * @param registeredSystem A system retrieved from the Service registry.
     * @return True if the two objects represent the same real-world system,
     *         false otherwise.
     */
    private boolean systemsMatch(PdeSystem entrySystem, SrSystem registeredSystem) {
        final Optional<String> name = entrySystem.systemName();
        if (name.isPresent() && name.get().equals(registeredSystem.systemName())) {
            return true;
        }
        // TODO: Look for a match using metadata as well
        return false;
    }

    /**
     *
     * @param alarm An alarm.
     * @param entrySystem A system retrieved from the Service registry.
     * @return True if the alarm refers to the given system, false otherwise.
     */
    private boolean alarmMatchesSystem(AlarmData alarm, SrSystem entrySystem) {
        // TODO: Look for a match using metadata as well
        return entrySystem.systemName().equals(alarm.systemName);
    }

    /**
     *
     * @param alarm An alarm.
     * @param entrySystem A Plant Description Entry system.
     * @return True if the alarm refers to the given system, false otherwise.
     */
    private boolean alarmMatchesSystem(AlarmData alarm, PdeSystem entrySystem) {
        Optional<String> systemName = entrySystem.systemName();
        boolean systemNamesMatch = systemName.isPresent() && systemName.get().equals(alarm.systemName);
        boolean systemIdsMatch = alarm.systemId != null && entrySystem.systemId().equals(alarm.systemId);
        // TODO: Look for a match using metadata as well
        return systemIdsMatch || systemNamesMatch;
    }

    private void checkSystems() {
        final var alarmManager = Locator.getAlarmManager();
        final List<SrSystem> registeredSystems = Locator.getSystemTracker().getSystems();
        final PlantDescriptionEntry activeEntry = pdTracker.activeEntry();
        final List<PdeSystem> pdSystems;

        pdSystems = (activeEntry == null) ? new ArrayList<>() : activeEntry.systems();

        clearAlarms(registeredSystems, pdSystems);

        // ---------------------------------------------------------------------
        // For each system in the active Plant Description...
        for (final var entrySystem : pdSystems) {
            String systemName = null;

            // ... check if the system is present in the Service Registry:
            for (final var registeredSystem : registeredSystems) {
                if (systemsMatch(entrySystem, registeredSystem)) {
                    systemName = registeredSystem.systemName();
                }
            }

            // If not, raise an alarm:
            if (systemName == null) {
                alarmManager.raiseSystemNotRegistered(entrySystem.systemName(), entrySystem.metadata());
            }
        }

        // ---------------------------------------------------------------------
        // For each registered system...
        for (final SrSystem registeredSystem : registeredSystems) {
            boolean presentInPd = false;

            // ... check if the system is present in the active Plant
            // Description:
            for (final var entrySystem : pdSystems) {
                if (systemsMatch(entrySystem, registeredSystem)) {
                    presentInPd = true;
                    break;
                }
            }

            // If not, raise an alarm:
            if (!presentInPd) {
                alarmManager.raiseAlarmBySystemName(
                    registeredSystem.systemName(),
                    AlarmManager.Cause.systemNotInDescription
                );
            }
        }
    }

    private void clearAlarms(List<SrSystem> registeredSystems, List<PdeSystem> pdSystems) {
        final var alarmManager = Locator.getAlarmManager();
        final List<AlarmData> activeAlarms = alarmManager.getActiveAlarmData(List.of(
            AlarmManager.Cause.systemNotInDescription,
            AlarmManager.Cause.systemNotRegistered
        ));

        // For each active "System not in description" and "System not
        // registered" alarm:

        for (final var alarm : activeAlarms) {

            // Check if the system is required by the current Plant
            // Description:
            boolean presentInPd = false;
            for (final var entrySystem : pdSystems) {

                if (alarmMatchesSystem(alarm, entrySystem)) {
                    presentInPd = true;
                    break;
                }
            }

            System.out.println("Missing system: " + alarm.systemName + ", " + alarm.systemId);

            // Check if the system is registered:
            boolean isRegistered = false;
            for (final var registeredSystem : registeredSystems) {
                System.out.println("Checking " + registeredSystem.systemName());
                if (alarmMatchesSystem(alarm, registeredSystem)) {
                    System.out.println("Match.");
                    isRegistered = true;
                    break;
                }
                System.out.println("No match.");
            }

            System.out.println("presentInPd: " + presentInPd + ", isRegistered: " + isRegistered);

            if (alarm.cause == AlarmManager.Cause.systemNotInDescription && (presentInPd || !isRegistered)) {
                alarmManager.clearAlarm(alarm);
            }
            else if (alarm.cause == AlarmManager.Cause.systemNotRegistered && (!presentInPd || isRegistered)) {
                System.out.println("Clearing systemNotRegistered!");
                alarmManager.clearAlarm(alarm);
                // alarmManager.clearAlarmBySystemId(alarm.systemId, AlarmManager.Cause.systemNotRegistered);
            }
        }
    }
}
