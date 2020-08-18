package eu.arrowhead.core.plantdescriptionengine.pdtracker;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;

/**
 * Unit test for the
 * {@link eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntry}
 * class.
 */
public class PlantDescriptionTrackerTest {

    @Test
    public void shouldReadEntriesFromBackingStore() throws PdStoreException {
        final PdStore store = new InMemoryPdStore();
        final List<Integer> entryIds = List.of(1, 2, 3);

        for (int id : entryIds) {
            store.write(TestUtils.createEntry(id));
        }

        final var pdTracker = new PlantDescriptionTracker(store);
        var storedEntries = pdTracker.getEntries();

        assertEquals(entryIds.size(), storedEntries.size());
        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
        }

        int id0 = entryIds.get(0);
        pdTracker.remove(id0);

        final var newPdTracker = new PlantDescriptionTracker(store);
        storedEntries = newPdTracker.getEntries();

        assertEquals(entryIds.size() - 1, storedEntries.size());
    }

    @Test
    public void shouldReturnEntryById() throws PdStoreException {
        int entryId = 16;
        final PlantDescriptionEntryDto entry = TestUtils.createEntry(entryId);
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        pdTracker.put(entry);

        final var storedEntry = pdTracker.get(entryId);

        assertNotNull(storedEntry);
        assertEquals(entryId, storedEntry.id(), 0);
    }

    @Test
    public void shouldReturnAllEntries() throws PdStoreException {

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final List<Integer> entryIds = List.of(16, 39, 244);

        for (int id : entryIds) {
            pdTracker.put(TestUtils.createEntry(id));
        }

        final var storedEntries = pdTracker.getEntries();

        assertEquals(entryIds.size(), storedEntries.size());

        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void shouldReturnListDto() throws PdStoreException {

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final List<Integer> entryIds = List.of(16, 39, 244);
        for (int id : entryIds) {
            pdTracker.put(TestUtils.createEntry(id));
        }

        final var storedEntries = pdTracker.getListDto();

        assertEquals(entryIds.size(), storedEntries.data().size());

        for (final var entry : storedEntries.data()) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void shouldRemoveEntries() throws PdStoreException {
        int entryId = 24;
        final PlantDescriptionEntryDto entry = TestUtils.createEntry(entryId);
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        pdTracker.put(entry);
        pdTracker.remove(entryId);
        final var storedEntry = pdTracker.get(entryId);

        assertNull(storedEntry);
    }

    @Test
    public void shouldTrackActiveEntry() throws PdStoreException {
        final Instant now = Instant.now();
        final var builder = new PlantDescriptionEntryBuilder()
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now);
        final var activeEntry = builder
            .id(1)
            .plantDescription("Plant Description A")
            .active(true)
            .build();
        final var inactiveEntry = builder
            .id(2)
            .plantDescription("Plant Description B")
            .active(false)
            .build();
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());

        pdTracker.put(activeEntry);
        pdTracker.put(inactiveEntry);

        assertNotNull(pdTracker.activeEntry());
        assertEquals(activeEntry.id(), pdTracker.activeEntry().id(), 0);

        pdTracker.remove(activeEntry.id());

        assertNull(pdTracker.activeEntry());
    }

    @Test
    public void shouldDeactivateEntry() throws PdStoreException {
        final int idA = 1;
        final int idB = 2;
        final Instant now = Instant.now();
        final var builder = new PlantDescriptionEntryBuilder()
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .active(true)
            .createdAt(now)
            .updatedAt(now);
        final var entryA = builder
            .id(idA)
            .plantDescription("Plant Description A")
            .build();
        final var entryB = builder
            .id(idB)
            .plantDescription("Plant Description B")
            .build();
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());

        pdTracker.put(entryA);

        assertTrue(pdTracker.get(idA).active());
        pdTracker.put(entryB);
        assertFalse(pdTracker.get(idA).active());
    }

    @Test
    public void shouldGenerateUniqueIds() throws PdStoreException {

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final List<PlantDescriptionEntryDto> entries = List.of(
            TestUtils.createEntry(pdTracker.getUniqueId()),
            TestUtils.createEntry(pdTracker.getUniqueId()),
            TestUtils.createEntry(pdTracker.getUniqueId())
        );

        for (final var entry : entries) {
            pdTracker.put(entry);
        }

        int uid = pdTracker.getUniqueId();

        for (final var entry : entries) {
            assertNotEquals(uid, entry.id());
        }
    }

    @Test
    public void shouldNotifyListeners() throws PdStoreException {

        final class Listener implements PlantDescriptionUpdateListener {

            int lastAdded = -1;
            int lastUpdated = -1;
            int lastRemoved = -1;

            @Override
            public void onPlantDescriptionAdded(PlantDescriptionEntry entry) {
                lastAdded = entry.id();
            }

            @Override
            public void onPlantDescriptionUpdated(PlantDescriptionEntry entry) {
                lastUpdated = entry.id();
            }

            @Override
            public void onPlantDescriptionRemoved(PlantDescriptionEntry entry) {
                lastRemoved = entry.id();
            }
        }

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var listener = new Listener();

        final int idA = 16;
        final int idB = 32;

        pdTracker.addListener(listener);
        pdTracker.put(TestUtils.createEntry(idA));
        pdTracker.put(TestUtils.createEntry(idA));

        // "Update" this entry by putting a new one with the same ID in the map:
        pdTracker.put(TestUtils.createEntry(idB));
        pdTracker.remove(idA);

        assertEquals(idB, listener.lastAdded, 0);
        assertEquals(idA, listener.lastRemoved, 0);
        assertEquals(idA, listener.lastUpdated, 0);
    }
}
