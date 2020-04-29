package eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore;

import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryDto;

/**
 * Interface for objects that read and write Plant Description Entries to
 * permanent storage, e.g. to file or to a database.
 */
public interface BackingStore {

    /**
     * @return Future containing all entries currently in the backing storage.
     */
    List<PlantDescriptionEntryDto> readEntries() throws BackingStoreException;

    /**
     * Writes a single entry to backing storag.
     * @param entry An entry to store.
     * @return Future that finishes when the request has been completed.
     */
    void write(final PlantDescriptionEntryDto entry) throws BackingStoreException;

    /**
     * Delete the specified entry from the backing store.
     * @param id ID of the entry to delete.
     */
    void remove(int id) throws BackingStoreException;

}