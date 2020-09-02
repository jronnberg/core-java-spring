package eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

/**
 * Class that reads and writes Orchestration rules to file.
 *
 * The PDE needs to keep track of which Orchestration rules it has created, and
 * to which Plant Description Entry each rule belongs. This information is
 * stored in memory, but it also needs to be persisted to permanent storage in
 * case the PDE is restarted. This class provides that functionality, writing
 * rules and their relationship to Plant Descriptions to file.
 */
public class FileRuleStore implements RuleStore {

    // File path to the directory for storing the IDs of created Orchestration
    // rules created by the PDE:
    private final String ruleFileDirectory;

    /**
     * Class constructor.
     *
     * @param ruleFileDirectory File path to the directory for storing rules.
     * @throws RuleStoreException
     */
    public FileRuleStore(final String descriptionDirectory) {
        Objects.requireNonNull(descriptionDirectory, "Expected path to Orchestrator Rule directory");
        this.ruleFileDirectory = descriptionDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Integer> readRules() throws RuleStoreException {
        final File file = new File(ruleFileDirectory);
        final var result = new HashSet<Integer>();

        if (!file.isFile()) {
            return result;
        }

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextInt()) {
                result.add(scanner.nextInt());
            }
        } catch (FileNotFoundException e) {
            throw new RuleStoreException(e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRules(Set<Integer> rules) throws RuleStoreException {

        final File file = new File(ruleFileDirectory + "/orchestration_rules.txt");

        try {
            // Create the file and parent directories, if they do not already
            // exist:
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            // Write each rule ID on a single line:
            final var writer = new BufferedWriter(new FileWriter(file));
            for (Integer rule : rules) {
                writer.write(rule.toString());
                writer.newLine();
            }

            writer.close();

        } catch (IOException e) {
            throw new RuleStoreException("Failed to write orchestration rule to file", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() throws RuleStoreException {
        new File(ruleFileDirectory).delete();
    }
}