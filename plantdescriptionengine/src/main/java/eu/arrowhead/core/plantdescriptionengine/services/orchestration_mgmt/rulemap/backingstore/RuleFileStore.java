package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap.backingstore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

/**
 * Interface for objects that read and write Orchestration rule info to file.
 *
 * The PDE needs to keep track of which Orchestration rules it has created, and
 * to which Plant Description Entry each rule belongs. This information is
 * stored in memory, but it also needs to be persisted to permanent storage in
 * case the PDE is restarted. This class provides that functionality, writing
 * rules and their relationship to Plant Descriptions to file.
 */
public class RuleFileStore implements RuleBackingStore {

    // File path to the directory for storing the IDs of created Orchestration
    // rules created by the PDE:
    private final String ruleDirectory;

    /**
     * Class constructor.
     *
     * @param ruleDirectory File path to the directory for storing rules.
     * @throws RuleBackingStoreException
     */
    public RuleFileStore(final String descriptionDirectory) {
        Objects.requireNonNull(descriptionDirectory, "Expected path to Orchestrator Rule directory");
        this.ruleDirectory = descriptionDirectory;
    }

    /**
     * @return The file path to use for reading or writing rules to disk.
     */
    private Path getFilePath(final int entryId) {
        return Paths.get(ruleDirectory, entryId + ".txt");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, Set<Integer>> readRules() throws RuleBackingStoreException {
        final File directory = new File(ruleDirectory);
        directory.mkdir();

        final File[] directoryListing = directory.listFiles();

        // Read all Plant Description entries into memory.
        if (directoryListing == null) {
            throw new RuleBackingStoreException(new FileNotFoundException());
        }

        final var result = new HashMap<Integer, Set<Integer>>();

        for (final File child : directoryListing) {
            // Extract the ID from the filename
            // (the filename will be on the form "<ID>.txt")
            final String filename = child.getName();
            int dotPosition = filename.indexOf(".");
            int entryId = Integer.parseInt(filename.substring(0, dotPosition));
            Set<Integer> rules = new HashSet<>();
            try (Scanner scanner = new Scanner(child)) {
                while (scanner.hasNextInt()) {
                    rules.add(scanner.nextInt());
                }
            } catch (FileNotFoundException e) {
                throw new RuleBackingStoreException(e);
            }
            result.put(entryId, rules);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int entryId, Set<Integer> rules) throws RuleBackingStoreException {
        final Path path = getFilePath(entryId);
        try {
            // Create the file and parent directories, if they do not already
            // exist:
            Files.createDirectories(path.getParent());
            final File file = path.toFile();

            if (!file.exists()) {
                file.createNewFile();
            }

            // Write each rule ID on a single line:
            final var writer = new BufferedWriter(new FileWriter(file));
            for (Integer rule : rules) {
                writer.write(rule.toString());
                writer.newLine();
            }

            writer.close();

        } catch (IOException e) {
            throw new RuleBackingStoreException("Failed to write orchestration rule to file", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePlantDescriptionRules(final int entryId) throws RuleBackingStoreException {
        final Path filepath = getFilePath(entryId);
        try {
            Files.delete(filepath);
        } catch (final IOException e) {
            throw new RuleBackingStoreException("Failed to delete orchestration rule file", e);
        }
    }
}