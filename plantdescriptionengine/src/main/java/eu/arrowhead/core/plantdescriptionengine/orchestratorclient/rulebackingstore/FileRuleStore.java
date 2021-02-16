package eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore;

import java.io.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

/**
 * Class that reads and writes Orchestration rules to file.
 * <p>
 * The PDE needs to keep track of which Orchestration rules it has created, and
 * to which Plant Description Entry each rule belongs. This information is
 * stored in memory, but it also needs to be persisted to permanent storage in
 * case the PDE is restarted. This class provides that functionality, writing
 * rules and their relationship to Plant Descriptions to file.
 */
public class FileRuleStore implements RuleStore {

    // File path to the directory for storing the IDs of created Orchestration
    // rules created by the PDE:
    private final String ruleStoreFile;

    /**
     * Class constructor.
     *
     * @param ruleStoreDirectory File path to the directory for storing rules.
     */
    public FileRuleStore(final String ruleStoreDirectory) {
        Objects.requireNonNull(ruleStoreDirectory, "Expected path to Orchestrator Rule directory");
        this.ruleStoreFile = ruleStoreDirectory + "/orchestration_rules.txt";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Integer> readRules() throws RuleStoreException {
        final File file = new File(ruleStoreFile);
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

        final File file = new File(ruleStoreFile);

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
    public void removeAll() {
        new File(ruleStoreFile).delete();
    }
}