package eu.arrowhead.core.plantdescriptionengine;

import eu.arrowhead.core.plantdescriptionengine.alarms.Alarm;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmCause;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.SystemUpdateListener;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystem;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionUpdateListener;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystem;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SystemMismatchDetector implements PlantDescriptionUpdateListener, SystemUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(SystemMismatchDetector.class);

    private final PlantDescriptionTracker pdTracker;
    private final SystemTracker systemTracker;
    private final AlarmManager alarmManager;

    public SystemMismatchDetector(
        PlantDescriptionTracker pdTracker,
        SystemTracker systemTracker,
        AlarmManager alarmManager
    ) {
        Objects.requireNonNull(pdTracker, "Expected Plant Description Tracker");
        Objects.requireNonNull(systemTracker, "Expected System Tracker");
        Objects.requireNonNull(alarmManager, "Expected Alarm Manager");

        this.pdTracker = pdTracker;
        this.systemTracker = systemTracker;
        this.alarmManager = alarmManager;
    }

    /**
     * Start monitoring Plant Descriptions and registered systems, raising
     * alarms whenever there is a mismatch, and clearing alarms when issues are
     * solved.
     */
    public void run() {
        pdTracker.addListener(this);
        systemTracker.addListener(this);

        // Initial check for mismatches:
        checkSystems();
    }

    @Override
    public void onPlantDescriptionAdded(PlantDescriptionEntry entry) {
        logger.debug("Entry '" + entry.plantDescription() + "' added, checking for inconcistencies...");
        checkSystems();
    }

    @Override
    public void onPlantDescriptionUpdated(PlantDescriptionEntry entry) {
        logger.debug("Entry '" + entry.plantDescription() + "' updated, checking for inconcistencies...");
        checkSystems();
    }

    @Override
    public void onPlantDescriptionRemoved(PlantDescriptionEntry entry) {
        logger.debug("Entry '" + entry.plantDescription() + "' removed, checking for inconcistencies...");
        checkSystems();
    }

    @Override
    public void onSystemAdded(SrSystem system) {
        logger.debug("System '" + system.systemName() + "' added, checking for inconcistencies...");
        checkSystems();
    }

    @Override
    public void onSystemRemoved(SrSystem system) {
        logger.debug("System '" + system.systemName() + "' removed, checking for inconcistencies...");
        checkSystems();
    }

    /**
     * @param entrySystem      A system in a Plant Description Entry.
     * @param registeredSystem A system retrieved from the Service registry.
     * @return True if the two objects represent the same real-world system,
     * false otherwise.
     */
    private boolean systemsMatch(PdeSystem entrySystem, SrSystem registeredSystem) {
        final Optional<String> name = entrySystem.systemName();
        if (name.isPresent()) {
            return name.get().equals(registeredSystem.systemName());
        } else {
            assert false;
            // TODO: This part of the code is never reached, since nameless
            // systems are not yet supported.
        }

        // TODO: Look for a match using metadata as well
        return false;
    }

    /**
     * @param alarm  An alarm.
     * @param system A system retrieved from the Service registry.
     * @return True if the alarm refers to the given system, false otherwise.
     */
    private boolean alarmMatchesSystem(Alarm alarm, SrSystem system) {
        // TODO: Look for a match using metadata as well
        return system.systemName().equals(alarm.systemName);
    }

    /**
     * @param alarm       An alarm.
     * @param entrySystem A system in a Plant Description Entry.
     * @return True if the alarm refers to the given system, false otherwise.
     */
    private boolean alarmMatchesSystem(Alarm alarm, PdeSystem entrySystem) {
        Optional<String> systemName = entrySystem.systemName();
        assert systemName.isPresent();
        boolean systemNamesMatch = systemName.get().equals(alarm.systemName);
        boolean systemIdsMatch = alarm.systemId != null && entrySystem.systemId().equals(alarm.systemId);
        // TODO: Look for a match using metadata as well
        return systemIdsMatch || systemNamesMatch;
    }

    /**
     * Checks that the systems in the Service Registry match those in the
     * currently active Plant Description. An alarm is raised for every
     * mismatch.
     */
    private void checkSystems() {
        final List<SrSystem> registeredSystems = systemTracker.getSystems();
        final PlantDescriptionEntry activeEntry = pdTracker.activeEntry();
        List<PdeSystem> pdSystems = new ArrayList<>();

        if (activeEntry != null) {
            pdSystems.addAll(pdTracker.getActiveSystems());
        }

        clearAlarms(registeredSystems, pdSystems);

        // ---------------------------------------------------------------------
        // For each system in the active Plant Description...
        for (final var entrySystem : pdSystems) {
            boolean matchFound = false;

            // ... check if the system is present in the Service Registry:
            for (final var registeredSystem : registeredSystems) {
                if (systemsMatch(entrySystem, registeredSystem)) {
                    matchFound = true;
                }
            }

            // If not, raise an alarm:
            if (!matchFound) {
                alarmManager.raiseSystemNotRegistered(
                    entrySystem.systemName().orElse(null), entrySystem.metadata().orElse(null));
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
                alarmManager.raiseSystemNotInDescription(registeredSystem.systemName());
            }
        }
    }

    private void clearAlarms(List<SrSystem> registeredSystems, List<PdeSystem> pdSystems) {
        final List<Alarm> activeAlarms = alarmManager.getActiveAlarmData(List.of(
            AlarmCause.systemNotInDescription,
            AlarmCause.systemNotRegistered
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

            // Check if the system is registered:
            boolean isRegistered = false;
            for (final var registeredSystem : registeredSystems) {
                if (alarmMatchesSystem(alarm, registeredSystem)) {
                    isRegistered = true;
                    break;
                }
            }

            if (alarm.cause == AlarmCause.systemNotInDescription) {

                if (presentInPd) {
                    alarmManager.clearAlarm(alarm);
                }
                if (!isRegistered) {
                    alarmManager.clearAlarm(alarm);
                }
            }
            if (alarm.cause == AlarmCause.systemNotRegistered) {

                if (!presentInPd) {
                    alarmManager.clearAlarm(alarm);
                }
                if (isRegistered) {
                    alarmManager.clearAlarm(alarm);
                }
            }
        }
    }
}
