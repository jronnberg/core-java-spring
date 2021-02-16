package eu.arrowhead.core.plantdescriptionengine.utils;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;

import java.util.List;

/**
 * Mock Plant Description backing store used for testing.
 * <p>
 * Created for development purposes, not to be used in production.
 */
public class MockPdStore implements PdStore {

    private final InMemoryPdStore store = new InMemoryPdStore();

    private boolean failOnNextRead = false;
    private boolean failOnNextWrite = false;
    private boolean failOnNextRemove = false;

    /**
     * @return A list of all entries currently in the backing store.
     * @throws PdStoreException If {@code setFailOnNextRead} has been called
     *                          since the last time this method was called.
     */
    @Override
    public List<PlantDescriptionEntryDto> readEntries() throws PdStoreException {
        if (failOnNextRead) {
            failOnNextRead = false;
            throw new PdStoreException("Mocked read failure");
        }
        return store.readEntries();
    }

    /**
     * Writes a single entry to backing store.
     *
     * @param entry An entry to store.
     * @throws PdStoreException If {@code setFailOnNextWrite} has been called
     *                          since the last time this method was called.
     */
    @Override
    public void write(final PlantDescriptionEntryDto entry) throws PdStoreException {
        if (failOnNextWrite) {
            failOnNextWrite = false;
            throw new PdStoreException("Mocked write failure");
        }
        store.write(entry);
    }

    /**
     * Delete the specified entry from the backing store.
     *
     * @param id ID of the entry to delete.
     * @throws PdStoreException If {@code setFailOnNextRemove} has been called
     *                          since the last time this method was called.
     */
    @Override
    public void remove(final int id) throws PdStoreException {
        if (failOnNextRemove) {
            failOnNextRemove = false;
            throw new PdStoreException("Mocked remove failure");
        }
        store.remove(id);
    }

    /**
     * If called, an exception will be thrown next time {@code read} is called.
     */
    public void setFailOnNextRead() {
        failOnNextRead = true;
    }

    /**
     * If called, an exception will be thrown next time {@code write} is called.
     */
    public void setFailOnNextWrite() {
        failOnNextWrite = true;
    }

    /**
     * If called, an exception will be thrown next time {@code remove} is
     * called.
     */
    public void setFailOnNextRemove() {
        failOnNextRemove = true;
    }

}