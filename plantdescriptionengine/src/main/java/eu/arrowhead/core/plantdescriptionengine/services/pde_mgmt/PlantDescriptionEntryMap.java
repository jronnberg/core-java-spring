package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryListDto;

/**
 * Object for keeping track of Plant Description entries.
 * Each instance Keeps a reference to a
 * {@link eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore},
 * which is used to store Plant Description Entries in some permanent storage
 * (e.g. to file a database).
 *
 * TODO: This class is used by the monitor service as well, so it should be
 *       moved out of the management package.
 */
public class PlantDescriptionEntryMap {

    // List of instances that need to be informed of any changes to Plant
    // Description Entries.
    List<PlantDescriptionUpdateListener> listeners = new ArrayList<>();

    // ID-to-entry mapping
    private Map<Float, PlantDescriptionEntryDto> entries = new ConcurrentHashMap<>();

    // Non-volatile storage for entries:
    private final BackingStore backingStore;

    // Integer for storing the next plant description entry ID to be used:
    private AtomicInteger nextId = new AtomicInteger();

    /**
     * Class constructor.
     *
     * @param backingStore Non-volatile storage for entries.
     * @throws BackingStoreException If backing store operations fail.
     */
    public PlantDescriptionEntryMap(BackingStore backingStore) throws BackingStoreException {
        Objects.requireNonNull(backingStore, "Expected backing store");
        this.backingStore = backingStore;

        // Read entries from non-volatile storage and
        // calculate the next free Plant Description Entry ID:
        float maxId = -1;
        for (var entry : backingStore.readEntries()) {
            maxId = Math.max(maxId, entry.id());
            entries.put(entry.id(), entry);
        }
        nextId.set(Math.round(maxId + 1));
    }

    /**
     * @return An unused Plant Description Entry ID.
     */
    public int getUniqueId() {
        return nextId.getAndIncrement();
    }

    /**
     * Stores the given entry in memory and in the backingstore.
     * Any registered {@code PlantDescriptionUpdateListener} are notified.
     *
     * @param entry Entry to store in the map.
     * @throws BackingStoreException If the entry is not successfully stored in
     *                               permanent storage. In this case, the entry
     *                               will not be stored in memory either, and no
     *                               listeners will be notified.
     */
    public void put(final PlantDescriptionEntryDto entry) throws BackingStoreException {
        backingStore.write(entry);

        final boolean isNew = !entries.containsKey(entry.id());
        final var currentlyActive = activeEntry();

        // Possible deactivate the currently active entry:
        if (entry.active() && currentlyActive != null) {
            final var deactivatedEntry = PlantDescriptionEntry.deactivated(currentlyActive);
            entries.put(deactivatedEntry.id(), deactivatedEntry);
            for (var listener : listeners) {
                listener.onPlantDescriptionUpdated(entry);
            }
        }

        entries.put(entry.id(), entry);

        for (var listener : listeners) {
            if (isNew) {
                listener.onPlantDescriptionAdded(entry);
            } else {
                listener.onPlantDescriptionUpdated(entry);
            }
        }
    }

    public PlantDescriptionEntryDto get(float id) {
        return entries.get(id);
    }

    /**
     * Removes the specified Plant Description Entry.
     * The entry is removed from memory and from the backing store.
     *
     * @param id ID of the entry to remove.
     * @throws BackingStoreException If the entry is not successfully removed
     *                               from permanent storage. In this case, the
     *                               entry will not be stored in memory either,
     *                               and no listeners will be notified.
     */
    public void remove(float id) throws BackingStoreException {
        backingStore.remove(id);
        var entry = entries.remove(id);

        // Notify listeners:
        for (var listener : listeners) {
            listener.onPlantDescriptionRemoved(entry);
        }
    }

    /**
     * @return A list of current Plant Description Entries.
     */
    public List<PlantDescriptionEntryDto> getEntries() {
        return new ArrayList<>(entries.values());
    }

    /**
     * @return A data transfer object representing the current list of Plant
     *         Description entries.
     */
    public PlantDescriptionEntryListDto getListDto() {
        var data = new ArrayList<>(entries.values());
        return new PlantDescriptionEntryListBuilder()
            .data(data)
            .count(data.size())
            .build();
    }

    /**
     * Registers another object to be notified whenever a Plant Description
     * Entry is added, updated or deleted.
     * @param listener
     */
    public void addListener(PlantDescriptionUpdateListener listener) {
        listeners.add(listener);
    }

    /**
     * @return The currently active entry, if any. Null there is none.
     */
    public PlantDescriptionEntry activeEntry() {
        for (var entry : entries.values()) {
            if (entry.active()) {
                return entry;
            }
        }
        return null;
    }

}