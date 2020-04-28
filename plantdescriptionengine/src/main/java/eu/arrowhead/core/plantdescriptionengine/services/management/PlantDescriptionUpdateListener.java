package eu.arrowhead.core.plantdescriptionengine.services.management;

import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryDto;

public interface PlantDescriptionUpdateListener {

    /*
    // TODO: The interface should look like this:
    void onPlantDescriptionAdded(PlantDescriptionEntry entry);
    void onPlantDescriptionUpdated(PlantDescriptionEntry entry);
    void onPlantDescriptionRemoved(int id);
    */

    // TODO: Not like this:
    void onUpdate(List<PlantDescriptionEntryDto> entries);
}