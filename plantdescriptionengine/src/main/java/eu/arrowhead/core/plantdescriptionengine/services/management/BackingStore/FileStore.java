package eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.DtoWriter;
import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PlantDescriptionEntryDto;
import se.arkalix.dto.DtoReadException;
import se.arkalix.dto.DtoWriteException;
import se.arkalix.dto.binary.ByteArrayReader;

public class FileStore implements BackingStore {

    // File path to the directory for storing JSON representations of plant
    // descriptions:
    private final String descriptionDirectory;

    /**
     * Class constructor.
     *
     * @param descriptionDirectory File path to the directory for storing Plant
     *                             Description.
     * @throws BackingStoreException
     */
    public FileStore(String descriptionDirectory) {
        Objects.requireNonNull(descriptionDirectory, "Expected path to Plant Description Entry directory");
        this.descriptionDirectory = descriptionDirectory;
    }

    /**
     * @return The file path to use for reading or writing a Plant Description
     *         Entry to disk.
     */
    private Path getFilePath(int entryId) {
        return Paths.get(descriptionDirectory, entryId + ".json");
    }

    /**
     * @return All entries in the backing storage.
     * @throws BackingStoreException
     */
    @Override
    public List<PlantDescriptionEntryDto> readEntries() throws BackingStoreException {
        File directory = new File(descriptionDirectory);
        directory.mkdir();

        File[] directoryListing = directory.listFiles();

        // Read all Plant Description entries into memory.
        if (directoryListing == null) {
            throw new BackingStoreException(new FileNotFoundException());
        }

        var result = new ArrayList<PlantDescriptionEntryDto>();
        for (File child : directoryListing) {
            byte[] bytes = null;
            try {
                bytes = Files.readAllBytes(child.toPath());
                result.add(PlantDescriptionEntryDto.readJson(new ByteArrayReader(bytes)));
            } catch (DtoReadException | IOException e) {
                throw new BackingStoreException(e);
            }
        }
        return result;
    }

        /**
     * Writes a single entry to backing storage.
     *
     * @param entry An entry to store.
     */
    @Override
    public void write(final PlantDescriptionEntryDto entry) throws BackingStoreException {
        Path path = getFilePath(entry.id());
        // Create the file and parent directories, if they do not already exist:
        try {
            Files.createDirectories(path.getParent());
            File file = path.toFile();
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new BackingStoreException(e);
                }
            }
            FileOutputStream out = new FileOutputStream(file);
            final var writer = new DtoWriter(out);
            entry.writeJson(writer);
            out.close();
        } catch (IOException | DtoWriteException e) {
            throw new BackingStoreException("Failed to write Plant Description Entry to file", e);
        }
    }

    @Override
    public void remove(int entryId) throws BackingStoreException {
        final Path filepath = getFilePath(entryId);
        try {
            Files.delete(filepath);
        } catch (IOException e) {
            throw new BackingStoreException("Failed to delete Plant Description Entry file", e);
        }
    }
}