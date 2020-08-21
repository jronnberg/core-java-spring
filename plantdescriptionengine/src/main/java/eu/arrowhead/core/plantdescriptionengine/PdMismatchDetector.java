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

    private boolean systemsMatch(PdeSystem entrySystem, SrSystem registeredSystem) {
        final Optional<String> name = entrySystem.systemName();
        if (name.isPresent() && name.get().equals(registeredSystem.systemName())) {
            return true;
        }
        // TODO: Look for a match using metadata as well?
        return false;
    }

    private void checkSystems() {
        final var alarmManager = Locator.getAlarmManager();
        final List<SrSystem> registeredSystems = Locator.getSystemTracker().getSystems();
        final PlantDescriptionEntry activeEntry = pdTracker.activeEntry();
        final List<PdeSystem> pdSystems;
        if (activeEntry == null) {
            pdSystems = new ArrayList<>();
        } else {
            pdSystems = activeEntry.systems();
        }

        for (final var entrySystem : pdSystems) {
            String systemName = null;

            for (final var registeredSystem : registeredSystems) {
                if (systemsMatch(entrySystem, registeredSystem)) {
                    systemName = registeredSystem.systemName();
                }
            }

            if (systemName == null) {
                // No corresponding system was found in the Service registry.
                // Raise an alarm:
                alarmManager.raiseAlarmBySystemId(entrySystem.systemId(), AlarmManager.Cause.systemNotRegistered);
            } else {
                alarmManager.clearAlarmBySystemName(systemName, AlarmManager.Cause.systemNotInDescription);
            }
        }

        for (final SrSystem registeredSystem : registeredSystems) {
            String systemId = null;
            for (final var entrySystem : pdSystems) {
                if (systemsMatch(entrySystem, registeredSystem)) {
                    systemId = entrySystem.systemId();
                }
            }

            if (systemId == null) {
                // The new system is not present in the currently active, Plant
                // Description, raise an alarm:
                alarmManager.raiseAlarmBySystemName(
                    registeredSystem.systemName(),
                    AlarmManager.Cause.systemNotInDescription
                );
            } else {
                // The system was found in the currently active Plant
                // Description. If there was previously an alarm indicating that
                // it was missing, clear it:
                alarmManager.clearAlarmBySystemId(systemId, AlarmManager.Cause.systemNotRegistered);
            }
        }
    }
}