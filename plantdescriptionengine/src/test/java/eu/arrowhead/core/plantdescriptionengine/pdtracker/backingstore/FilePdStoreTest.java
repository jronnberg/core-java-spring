package eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;

/**
 * Unit test for the
 * {@link eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.FilePdStore}
 * class.
 */
public class FilePdStoreTest {

    private String entryDirectory = "test-temp-data";

    private void deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }

    @AfterEach
    public void removeTestDirectory() {
        deleteDirectory(new File(entryDirectory));
    }

    @Test
    public void ShouldReadWithoutEntries() throws PdStoreException {
        final PdStore store = new FilePdStore(entryDirectory);
        var storedEntries = store.readEntries();
        assertTrue(storedEntries.isEmpty());
    }

    @Test
    public void ShouldWriteEntries() throws PdStoreException {
        final PdStore store = new FilePdStore(entryDirectory);
        final List<Integer> entryIds = List.of(1, 2, 3);

        for (int id : entryIds) {
            store.write(TestUtils.createEntry(id));
        }

        var storedEntries = store.readEntries();
        assertEquals(storedEntries.size(), entryIds.size());

        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void ShouldRemoveEntries() throws PdStoreException {
        final PdStore store = new FilePdStore(entryDirectory);
        final List<Integer> entryIds = List.of(1, 2, 3);

        for (int id : entryIds) {
            store.write(TestUtils.createEntry(id));
        }

        int id0 = entryIds.get(0);
        store.remove(id0);

        var storedEntries = store.readEntries();
        assertEquals(storedEntries.size(), entryIds.size() - 1);

        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
            assertNotEquals(entry.id(), id0);
        }
    }
}