package eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class FileRuleStoreTest {

    private String entryDirectory = "test-temp-data";

    private void deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }

    @AfterEach
    public void removeTestDirectory() {
        deleteDirectory(new File(entryDirectory));
    }

    @Test
    public void shouldWriteRules() throws RuleStoreException {
        final var store = new FileRuleStore(entryDirectory);
        final Set<Integer> rules = Set.of(1, 2, 3);

        store.setRules(rules);

        var storedRules = store.readRules();
        assertEquals(rules, storedRules);
    }


    @Test
    public void shouldRemoveRules() throws RuleStoreException {
        final var store = new FileRuleStore(entryDirectory);
        final Set<Integer> rules = Set.of(1, 2, 3);

        store.setRules(rules);
        store.removeAll();

        var storedRules = store.readRules();
        assertTrue(storedRules.isEmpty());
    }

}
