package eu.arrowhead.core.plantdescriptionengine.services.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore.BackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryListDto;

/**
 * Object for keeping track of Plant Description entries.
 * Keeps a reference to a PlantDescriptionStore instance, which is used to store
 * Plant Description Entries in some permanent storage (e.g. to file a
 * database).
 */
public class PlantDescriptionEntryMap {

    // List of instances that need to be informed of any changes to Plant
    // Description Entries.
    List<PlantDescriptionUpdateListener> listeners = new ArrayList<>();

    private Map<Integer, PlantDescriptionEntryDto> entries = new ConcurrentHashMap<>();

    // Non-volatile storage for entries:
    private final BackingStore backingStore;

    // Integer for storing the next plant description entry ID to be used:
    private AtomicInteger nextId = new AtomicInteger();

    /**
     * Class constructor.
     * @param BackingStore
     * @throws BackingStoreException Non-volatile storage for entries.
     */
    public PlantDescriptionEntryMap(BackingStore backingStore) throws BackingStoreException {
        Objects.requireNonNull(backingStore, "Expected backing store");
        this.backingStore = backingStore;

        // Calculate the next free Plant Description Entry ID:
        int maxId = -1;
        for (var entry : backingStore.readEntries()) {
            maxId = Math.max(maxId, entry.id());
        }
        nextId.set(maxId + 1);
        System.out.println("Using nextId " + nextId.get());
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
     * @param entry Entry to store in the map.
     * @throws BackingStoreException
     */
    public void put(final PlantDescriptionEntryDto entry) throws BackingStoreException {
        backingStore.write(entry);
        entries.put(entry.id(), entry);

        // Notify listeners:
        for (var listener : listeners) {
            listener.onUpdate(getEntries());
        }
    }

    public PlantDescriptionEntryDto get(int id) {
        return entries.get(id);
    }

    public void remove(int id) throws BackingStoreException {
        backingStore.remove(id);
        entries.remove(id);
        for (var listener : listeners) {
            listener.onUpdate(getEntries());
        }
    }

    public List<PlantDescriptionEntryDto> getEntries() {
        return new ArrayList<>(entries.values());
    }

    public PlantDescriptionEntryListDto getListDto() {
        return new PlantDescriptionEntryListBuilder()
            .data(new ArrayList<>(entries.values()))
            .build();
    }

    public void addListener(PlantDescriptionUpdateListener listener) {
        listeners.add(listener);
    }

}