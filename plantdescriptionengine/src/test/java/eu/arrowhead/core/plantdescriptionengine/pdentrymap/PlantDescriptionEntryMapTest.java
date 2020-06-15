package eu.arrowhead.core.plantdescriptionengine.pdentrymap;

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

import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.BackingStore;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.InMemoryBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;

/**
 * Unit test for the
 * {@link eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntry}
 * class.
 */
public class PlantDescriptionEntryMapTest {

    @Test
    public void shouldReadEntriesFromBackingStore() throws BackingStoreException {
        final BackingStore store = new InMemoryBackingStore();
        final List<Float> entryIds = List.of(1f, 2f, 3f);

        for (float id : entryIds) {
            store.write(TestUtils.createEntry(id));
        }

        final var entryMap = new PlantDescriptionEntryMap(store);
        var storedEntries = entryMap.getEntries();

        assertEquals(entryIds.size(), storedEntries.size());
        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
        }

        float id0 = entryIds.get(0);
        entryMap.remove(id0);

        final var newEntryMap = new PlantDescriptionEntryMap(store);
        storedEntries = newEntryMap.getEntries();

        assertEquals(entryIds.size() - 1, storedEntries.size());
    }

    @Test
    public void shouldReturnEntryById() throws BackingStoreException {
        int entryId = 16;
        final PlantDescriptionEntryDto entry = TestUtils.createEntry(entryId);
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        entryMap.put(entry);

        final var storedEntry = entryMap.get(entryId);

        assertNotNull(storedEntry);
        assertEquals(entryId, storedEntry.id(), 0);
    }

    @Test
    public void shouldReturnAllEntries() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final List<Float> entryIds = List.of(16f, 39f, 244f);

        for (float id : entryIds) {
            entryMap.put(TestUtils.createEntry(id));
        }

        final var storedEntries = entryMap.getEntries();

        assertEquals(entryIds.size(), storedEntries.size());

        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void shouldReturnListDto() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final List<Float> entryIds = List.of(16f, 39f, 244f);
        for (float id : entryIds) {
            entryMap.put(TestUtils.createEntry(id));
        }

        final var storedEntries = entryMap.getListDto();

        assertEquals(entryIds.size(), storedEntries.data().size());

        for (final var entry : storedEntries.data()) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void shouldRemoveEntries() throws BackingStoreException {
        float entryId = 24f;
        final PlantDescriptionEntryDto entry = TestUtils.createEntry(entryId);
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        entryMap.put(entry);
        entryMap.remove(entryId);
        final var storedEntry = entryMap.get(entryId);

        assertNull(storedEntry);
    }

    @Test
    public void shouldTrackActiveEntry() throws BackingStoreException {
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
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        entryMap.put(activeEntry);
        entryMap.put(inactiveEntry);

        assertNotNull(entryMap.activeEntry());
        assertEquals(activeEntry.id(), entryMap.activeEntry().id(), 0);

        entryMap.remove(activeEntry.id());

        assertNull(entryMap.activeEntry());
    }

    @Test
    public void shouldDeactivateEntry() throws BackingStoreException {
        final float idA = 1f;
        final float idB = 2f;
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
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        entryMap.put(entryA);

        assertTrue(entryMap.get(idA).active());
        entryMap.put(entryB);
        assertFalse(entryMap.get(idA).active());
    }

    @Test
    public void shouldGenerateUniqueIds() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final List<PlantDescriptionEntryDto> entries = List.of(
            TestUtils.createEntry(entryMap.getUniqueId()),
            TestUtils.createEntry(entryMap.getUniqueId()),
            TestUtils.createEntry(entryMap.getUniqueId())
        );

        for (final var entry : entries) {
            entryMap.put(entry);
        }

        int uid = entryMap.getUniqueId();

        for (final var entry : entries) {
            assertNotEquals(uid, entry.id());
        }
    }

    @Test
    public void shouldNotifyListeners() throws BackingStoreException {

        final class Listener implements PlantDescriptionUpdateListener {

            float lastAdded = -1f;
            float lastUpdated = -1f;
            float lastRemoved = -1f;

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

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var listener = new Listener();

        final int idA = 16;
        final int idB = 32;

        entryMap.addListener(listener);
        entryMap.put(TestUtils.createEntry(idA));
        entryMap.put(TestUtils.createEntry(idA));

        // "Update" this entry by putting a new one with the same ID in the map:
        entryMap.put(TestUtils.createEntry(idB));
        entryMap.remove(idA);

        assertEquals(idB, listener.lastAdded, 0);
        assertEquals(idA, listener.lastRemoved, 0);
        assertEquals(idA, listener.lastUpdated, 0);
    }
}
