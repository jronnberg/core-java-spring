package eu.arrowhead.core.plantdescriptionengine.services.management;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.DtoWriter;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryListDto;
import se.arkalix.dto.DtoReadException;
import se.arkalix.dto.DtoWriteException;
import se.arkalix.dto.binary.ByteArrayReader;

/**
 * Data store for keeping track of Plant Description entries.
 *
 * Before an instance of this class can be used, it must be initialized using
 * {@code readEntries}, which loads any existing entries from permanent storage.
 * Failure to do so will result in an {@code IllegalStateException}.
 */
public class PlantDescriptionEntryStore {

    List<PlantDescriptionUpdateListener> listeners = new ArrayList<>();

    boolean initialized = false;

    // File path to the directory for storing JSON representations of plant
    // descriptions:
    final String descriptionDirectory;

    private Map<Integer, PlantDescriptionEntryDto> entries = new ConcurrentHashMap<>();

    // Integer for storing the next plant description entry ID to be used:
    private AtomicInteger nextId = new AtomicInteger(0);

    public PlantDescriptionEntryStore(String descriptionDirectory) {
        Objects.requireNonNull(descriptionDirectory, "Expected path to Plant Description Entry directory");
        this.descriptionDirectory = descriptionDirectory;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("This instance has not been initialized using the 'readEntries' method.");
        }
    }

    /**
     * @return A new Plant Description Entry ID.
     */
    public int getNextId() {
        ensureInitialized();
        return nextId.getAndIncrement();
    }

    /**
     * Clears current state and reads Plant Description Entries from file.
     *
     * @throws IOException
     * @throws DtoReadException
     */
    public void readEntries() throws IOException, DtoReadException {
        initialized = false;
        entries.clear();
        File directory = new File(descriptionDirectory);
        directory.mkdir();

        File[] directoryListing = directory.listFiles();
        int greatestId = -1;

        // Read all Plant Description entries into memory.
        if (directoryListing != null) {
            for (File child : directoryListing) {
                byte[] bytes = Files.readAllBytes(child.toPath());
                PlantDescriptionEntryDto entry = PlantDescriptionEntryDto.readJson(new ByteArrayReader(bytes));
                entries.put(entry.id(), entry);

                // Keep track of the greatest ID currently in use.
                if (entry.id() > greatestId) {
                    greatestId = entry.id();
                }
            }
            nextId.set(greatestId + 1);
            initialized = true;
        } else {
            throw new IOException(descriptionDirectory + " is not a valid directory.");
        }
    }

    private void writeEntryFile(final PlantDescriptionEntryDto entry) throws DtoWriteException, IOException {
        // TODO: Make non-blocking (return Future)
        ensureInitialized();
        Path path = Paths.get(getFilePath(entry.id()));
        // Create the file and parent directories, if they do not already exist:
        Files.createDirectories(path.getParent());
        File file = path.toFile();
        if (!file.exists()) {
            file.createNewFile();
        }
        final FileOutputStream out = new FileOutputStream(file);
        final DtoWriter writer = new DtoWriter(out);
        entry.writeJson(writer);
        out.close();
    }

    private void deleteEntryFile(int entryId) throws IOException {
        ensureInitialized();
        // TODO: Make non-blocking (return Future)
        final String filename = getFilePath(entryId);
        Files.delete(Paths.get(filename));
    }

    /**
     * @return The path to use for writing Plant Description Entries to disk.
     */
    private String getFilePath(int entryId) {
        return descriptionDirectory + entryId + ".json";
    }

    public void put(final PlantDescriptionEntryDto entry) throws DtoWriteException, IOException {
        boolean isNew = !entries.containsKey(entry.id());
        ensureInitialized();
        writeEntryFile(entry);
        entries.put(entry.id(), entry);

        // Notify listeners:
        for (var listener : listeners) {
            listener.onUpdate(getEntries());
        }

        /*
        if (isNew) { // TODO: Do it like this instead:
            for (var listener : listeners) {
                listener.onPlantDescriptionAdded(entry);
            }
        } else {
            for (var listener : listeners) {
                listener.onPlantDescriptionUpdated(entry);
            }
        }
        */
    }

    public PlantDescriptionEntryDto get(int id) {
        ensureInitialized();
        return entries.get(id);
    }

    public void remove(int id) throws IOException {
        ensureInitialized();
        deleteEntryFile(id);
        entries.remove(id);
        for (var listener : listeners) {
            // listener.onPlantDescriptionRemoved(id);
            listener.onUpdate(getEntries());
        }
    }

    public PlantDescriptionEntryListDto getListDto() {
        ensureInitialized();
        return new PlantDescriptionEntryListBuilder()
            .data(new ArrayList<>(entries.values()))
            .build();
    }

    public List<PlantDescriptionEntryDto> getEntries() {
        ensureInitialized();
        return new ArrayList<>(entries.values());
    }

    public void addListener(PlantDescriptionUpdateListener listener) {
        listeners.add(listener);
    }

}