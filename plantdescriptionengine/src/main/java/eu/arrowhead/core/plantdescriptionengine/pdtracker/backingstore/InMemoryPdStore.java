package eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;

/**
 * Backing store that only stores data in memory.
 *
 * Created for development purposes, not to be used in production.
 */
public class InMemoryPdStore implements PdStore {

    // ID-to-entry map:
    private Map<Integer, PlantDescriptionEntryDto> entries = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlantDescriptionEntryDto> readEntries() throws PdStoreException {
        return new ArrayList<PlantDescriptionEntryDto>(entries.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final PlantDescriptionEntryDto entry) throws PdStoreException {
        entries.put(entry.id(), entry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final int id) throws PdStoreException {
        entries.remove(id);
    }

}