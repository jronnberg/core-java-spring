package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore;

/*
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
*/

public class SqlRuleStoreTest {

    /*
    TODO: Add these tests

    private final String DRIVER_NAME = "com.mysql.cj.jdbc.Driver";
    private final String CONNECTION_URL = "jdbc:mysql://localhost/test_pde?serverTimezone=UTC";
    private final String USERNAME = "root";
    private final String PASSWORD = "password";

    @Test
    public void shouldWriteRules() throws RuleStoreException {
        final SqlRuleStore store = new SqlRuleStore();
        store.init(DRIVER_NAME, CONNECTION_URL, USERNAME, PASSWORD);
        final Set<Integer> rules = Set.of(1, 2, 3);

        store.setRules(rules);

        final Set<Integer> storedRules = store.readRules();
        assertEquals(rules, storedRules);
    }

    @Test
    public void shouldRemoveRules() throws RuleStoreException {
        final SqlRuleStore store = new SqlRuleStore();
        store.init(DRIVER_NAME, CONNECTION_URL, USERNAME, PASSWORD);
        final Set<Integer> rules = Set.of(1, 2, 3);

        store.setRules(rules);
        store.removeAll();

        final Set<Integer> storedRules = store.readRules();
        assertTrue(storedRules.isEmpty());
    }

    */

}
