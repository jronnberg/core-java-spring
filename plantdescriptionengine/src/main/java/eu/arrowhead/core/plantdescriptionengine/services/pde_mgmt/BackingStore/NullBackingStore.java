package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore;

import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;

/**
 * Dummy backing store that does not save any data.
 */
public class NullBackingStore implements BackingStore {

    /**
     * @return An empty list.
     */
    @Override
    public List<PlantDescriptionEntryDto> readEntries() throws BackingStoreException {
        return new ArrayList<>();
    }

    /**
     * No-op.
     */
    @Override
    public void write(PlantDescriptionEntryDto entry) throws BackingStoreException {}

    /**
     * No-op.
     */
    @Override
    public void remove(int id) throws BackingStoreException {}

}