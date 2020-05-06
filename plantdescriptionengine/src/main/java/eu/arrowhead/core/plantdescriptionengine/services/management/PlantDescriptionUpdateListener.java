package eu.arrowhead.core.plantdescriptionengine.services.management;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntry;

public interface PlantDescriptionUpdateListener {
    void onPlantDescriptionAdded(PlantDescriptionEntry entry);
    void onPlantDescriptionUpdated(PlantDescriptionEntry entry);
    void onPlantDescriptionRemoved(PlantDescriptionEntry entry);
}