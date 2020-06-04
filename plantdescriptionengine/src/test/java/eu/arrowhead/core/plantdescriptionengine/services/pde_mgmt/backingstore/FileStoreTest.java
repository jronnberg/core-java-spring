package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;

/**
 * Unit test for the
 * {@link eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.FileStore}
 * class.
 */
public class FileStoreTest {

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

    @After
    public void removeTestDirectory() {
        deleteDirectory(new File(entryDirectory));
    }


    @Test
    public void ShouldWriteEntries() throws BackingStoreException {
        final BackingStore store = new FileStore(entryDirectory);
        final List<Float> entryIds = List.of(1f, 2f, 3f);

        for (float id : entryIds) {
            store.write(TestUtils.createEntry(id));
        }

        var storedEntries =  store.readEntries();
        assertEquals(storedEntries.size(), entryIds.size());

        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void ShouldRemoveEntries() throws BackingStoreException {
        final BackingStore store = new FileStore(entryDirectory);
        final List<Float> entryIds = List.of(1f, 2f, 3f);

        for (float id : entryIds) {
            store.write(TestUtils.createEntry(id));
        }

        float id0 = entryIds.get(0);
        store.remove(id0);

        var storedEntries = store.readEntries();
        assertEquals(storedEntries.size(), entryIds.size() - 1);

        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
            assertNotEquals(entry.id(), id0);
        }
    }

}