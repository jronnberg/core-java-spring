package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore;

import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;

/**
 * Interface for objects that read and write Plant Description Entries to
 * permanent storage, e.g. to file or to a database.
 */
public interface BackingStore {

    /**
     * @return A list of all entries currently in the backing store.
     */
    List<PlantDescriptionEntryDto> readEntries() throws BackingStoreException;

    /**
     * Writes a single entry to backing store.
     * @param entry An entry to store.
     */
    void write(final PlantDescriptionEntryDto entry) throws BackingStoreException;

    /**
     * Delete the specified entry from the backing store.
     * @param id ID of the entry to delete.
     */
    void remove(int id) throws BackingStoreException;

}