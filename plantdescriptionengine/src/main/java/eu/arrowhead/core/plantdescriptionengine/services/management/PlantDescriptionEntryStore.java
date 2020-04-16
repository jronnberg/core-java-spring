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

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.DtoWriter;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryListDto;
import se.arkalix.dto.DtoReadException;
import se.arkalix.dto.DtoWriteException;
import se.arkalix.dto.binary.ByteArrayReader;

public class PlantDescriptionEntryStore {

    // File path to the directory for storing JSON representations of plant
    // descriptions:
    final String descriptionDirectory;

    private Map<Integer, PlantDescriptionEntryDto> entries = new ConcurrentHashMap<>();

    public PlantDescriptionEntryStore(String descriptionDirectory) {
        Objects.requireNonNull(descriptionDirectory, "Expected path to Plant Description Entry directory");
        this.descriptionDirectory = descriptionDirectory;
    }

    /**
     * Clears current state and reads Plant Description Entries from file.
     *
     * @throws IOException
     * @throws DtoReadException
     */
    public void readEntries() throws IOException, DtoReadException {
        // TODO: Make non-blocking (return Future)
        entries.clear();
        File directory = new File(descriptionDirectory);
        directory.mkdir();

        File[] directoryListing = directory.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                byte[] bytes = Files.readAllBytes(child.toPath());
                PlantDescriptionEntryDto entry = PlantDescriptionEntryDto.readJson(new ByteArrayReader(bytes));
                entries.put(entry.id(), entry);
            }
        } else {
            throw new IOException(descriptionDirectory + " is not a valid directory.");
        }
    }

    private void writeEntryFile(final PlantDescriptionEntryDto entry) throws DtoWriteException, IOException {
        // TODO: Make non-blocking (return Future)

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
        writeEntryFile(entry);
        entries.put(entry.id(), entry);
    }

    public PlantDescriptionEntryDto get(int id) {
        return entries.get(id);
    }

    public void remove(int id) throws IOException {
        deleteEntryFile(id);
        entries.remove(id);
    }

    public PlantDescriptionEntryListDto getListDto() {
        return new PlantDescriptionEntryListBuilder()
            .data(new ArrayList<>(entries.values()))
            .build();
    }

    public List<PlantDescriptionEntryDto> getEntries() {
        return new ArrayList<>(entries.values());
    }

    // TODO: Move filtering and sorting from PlantDescriptionEntry.java to here?

}