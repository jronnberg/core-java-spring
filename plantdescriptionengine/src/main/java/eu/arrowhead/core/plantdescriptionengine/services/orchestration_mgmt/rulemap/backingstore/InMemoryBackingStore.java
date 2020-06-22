package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap.backingstore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backing store that only stores data in memory.
 *
 * Created for development purposes, not to be used in production.
 */
public class InMemoryBackingStore implements RuleBackingStore {

    // ID-to-rule map:
    private Map<Integer, Set<Integer>> rulesPerEntry = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, Set<Integer>> readRules() throws RuleBackingStoreException {
        Map<Integer, Set<Integer>> result = new HashMap<>();
        result.putAll(rulesPerEntry);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int entryId, Set<Integer> rules) throws RuleBackingStoreException {
        rulesPerEntry.put(entryId, rules);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePlantDescriptionRules(final int entryId) throws RuleBackingStoreException {
        rulesPerEntry.remove(entryId);
    }

}