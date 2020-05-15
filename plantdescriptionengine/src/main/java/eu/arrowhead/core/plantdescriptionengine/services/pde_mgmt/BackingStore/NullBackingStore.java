package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore;

import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;

/**
 * Dummy backing store that does not save any data.
 */
public class NullBackingStore implements BackingStore {

    @Override
    public List<PlantDescriptionEntryDto> readEntries() throws BackingStoreException {
        return new ArrayList<>();
    }

    @Override
    public void write(PlantDescriptionEntryDto entry) throws BackingStoreException {}

    @Override
    public void remove(int id) throws BackingStoreException {}

}