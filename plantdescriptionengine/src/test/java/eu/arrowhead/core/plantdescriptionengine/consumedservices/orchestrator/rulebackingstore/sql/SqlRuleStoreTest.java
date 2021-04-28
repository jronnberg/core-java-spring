package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.sql;

import org.junit.jupiter.api.Test;

import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.RuleStoreException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SqlRuleStoreTest {

    @Test
    public void shouldWriteRules() throws RuleStoreException {
        final SqlRuleStore store = new SqlRuleStore();
        store.init();
        final Set<Integer> rules = Set.of(1, 2, 3);

        store.setRules(rules);

        final Set<Integer> storedRules = store.readRules();
        assertEquals(rules, storedRules);
    }

    @Test
    public void shouldRemoveRules() throws RuleStoreException {
        final SqlRuleStore store = new SqlRuleStore();
        store.init();
        final Set<Integer> rules = Set.of(1, 2, 3);

        store.setRules(rules);
        store.removeAll();

        final Set<Integer> storedRules = store.readRules();
        assertTrue(storedRules.isEmpty());
    }

    @Test
    public void shouldRequireInitialization() {
        final Exception exception = assertThrows(IllegalStateException.class,
            () -> new SqlRuleStore().readRules());
        assertEquals("SqlRuleStore has not been initialized.", exception.getMessage());
    }

}
