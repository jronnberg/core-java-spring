package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;

/**
 * Backing store that only stores data in memory.
 *
 * Created for development purposes, not to be used in production.
 */
public class InMemoryBackingStore implements BackingStore {

    // ID-to-entry map:
    private Map<Float, PlantDescriptionEntryDto> entries = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlantDescriptionEntryDto> readEntries() throws BackingStoreException {
        return new ArrayList<PlantDescriptionEntryDto>(entries.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final PlantDescriptionEntryDto entry) throws BackingStoreException {
        entries.put(entry.id(), entry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final float id) throws BackingStoreException {
        entries.remove(id);
    }

}