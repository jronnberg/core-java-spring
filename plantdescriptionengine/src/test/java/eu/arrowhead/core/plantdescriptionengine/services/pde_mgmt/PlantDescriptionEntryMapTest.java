package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.BackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.InMemoryBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.NullBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;

/**
 * Unit test for the
 * {@link eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntry}
 * class.
 */
public class PlantDescriptionEntryMapTest {

    @Test
    public void shouldReadEntriesFromBackingStore() throws BackingStoreException {
        final BackingStore store = new InMemoryBackingStore();
        final List<Integer> entryIds = List.of(1, 2, 3);

        for (int id : entryIds) {
            store.write(Utils.createEntry(id));
        }

        final var entryMap = new PlantDescriptionEntryMap(store);
        var storedEntries = entryMap.getEntries();

        assertEquals(storedEntries.size(), entryIds.size());
        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
        }

        int id0 = entryIds.get(0);
        entryMap.remove(id0);

        final var newEntryMap = new PlantDescriptionEntryMap(store);
        storedEntries = newEntryMap.getEntries();

        assertEquals(storedEntries.size(), entryIds.size() - 1);
    }

    @Test
    public void shouldReturnEntryById() throws BackingStoreException {
        int entryId = 16;
        final PlantDescriptionEntryDto entry = Utils.createEntry(entryId);
        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        entryMap.put(entry);

        final var storedEntry = entryMap.get(entryId);

        assertNotNull(storedEntry);
        assertEquals(entryId, storedEntry.id());
    }

    @Test
    public void shouldReturnAllEntries() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        final List<Integer> entryIds = List.of(16, 39, 244);

        for (int id : entryIds) {
            entryMap.put(Utils.createEntry(id));
        }

        final var storedEntries = entryMap.getEntries();

        assertEquals(entryIds.size(), storedEntries.size());

        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void shouldReturnListDto() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        final List<Integer> entryIds = List.of(16, 39, 244);
        for (int id : entryIds) {
            entryMap.put(Utils.createEntry(id));
        }

        final var storedEntries = entryMap.getListDto();

        assertEquals(entryIds.size(), storedEntries.data().size());

        for (final var entry : storedEntries.data()) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void shouldRemoveEntries() throws BackingStoreException {
        int entryId = 24;
        final PlantDescriptionEntryDto entry = Utils.createEntry(entryId);
        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
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
        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());

        entryMap.put(activeEntry);
        entryMap.put(inactiveEntry);

        assertNotNull(entryMap.activeEntry());
        assertEquals(activeEntry.id(), entryMap.activeEntry().id());

        entryMap.remove(activeEntry.id());

        assertNull(entryMap.activeEntry());
    }

    @Test
    public void shouldGenerateUniqueIds() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        final List<PlantDescriptionEntryDto> entries = List.of(
            Utils.createEntry(entryMap.getUniqueId()),
            Utils.createEntry(entryMap.getUniqueId()),
            Utils.createEntry(entryMap.getUniqueId())
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

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        final var listener = new Listener();

        final int idA = 16;
        final int idB = 32;

        entryMap.addListener(listener);
        entryMap.put(Utils.createEntry(idA));
        entryMap.put(Utils.createEntry(idA));

        // "Update" this entry by putting a new one with the same ID in the map:
        entryMap.put(Utils.createEntry(idB));
        entryMap.remove(idA);

        assertEquals(idB, listener.lastAdded);
        assertEquals(idA, listener.lastRemoved);
        assertEquals(idA, listener.lastUpdated);
    }
}
