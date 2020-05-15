package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.util.ArrayList;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.NullBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;

/**
 * Unit test for the
 * {@link eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntry}
 * class.
 */
public class PlantDescriptionEntryMapTest {

    private PlantDescriptionEntryDto createEntry(int id) {
        final Instant now = Instant.now();
        return new PlantDescriptionEntryBuilder()
            .id(id)
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    @Test
    public void shouldStoreEntries() throws BackingStoreException
    {
        int entryId = 16;
        final PlantDescriptionEntryDto entry = createEntry(entryId);
        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        entryMap.put(entry);
        final var storedEntry = entryMap.get(entryId);

        // TODO: Implement an 'equals' method for PlantDescriptionEntry's, and
        // use that to check that the entries match?
        assertNotNull(storedEntry);
        assertEquals(entryId, storedEntry.id());
    }

    @Test
    public void shouldRemoveEntries() throws BackingStoreException
    {
        int entryId = 24;
        final PlantDescriptionEntryDto entry = createEntry(entryId);
        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        entryMap.put(entry);
        entryMap.remove(entryId);
        final var storedEntry = entryMap.get(entryId);

        assertNull(storedEntry);
    }

    @Test
    public void shouldTrackActiveEntry() throws BackingStoreException
    {
        Instant now = Instant.now();

        final var builder = new PlantDescriptionEntryBuilder()
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now);

            final var activeEntry = builder.id(1)
            .plantDescription("Plant Description A")
            .active(true)
            .build();

        final var inactiveEntry = builder.id(2)
            .plantDescription("Plant Description B")
            .active(false)
            .build();

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());

        entryMap.put(activeEntry);
        entryMap.put(inactiveEntry);

        assertEquals(activeEntry.id(), entryMap.activeEntry().id());

    }
}
