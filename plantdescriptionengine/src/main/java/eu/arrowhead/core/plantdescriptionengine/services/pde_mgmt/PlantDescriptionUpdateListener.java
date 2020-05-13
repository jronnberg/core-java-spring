package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;

public interface PlantDescriptionUpdateListener {
    void onPlantDescriptionAdded(PlantDescriptionEntry entry);
    void onPlantDescriptionUpdated(PlantDescriptionEntry entry);
    void onPlantDescriptionRemoved(PlantDescriptionEntry entry);
}