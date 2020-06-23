package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulebackingstore;

import java.util.HashSet;
import java.util.Set;

/**
 * Backing store that only stores data in memory.
 *
 * Created for development purposes, not to be used in production.
 */
public class InMemoryRuleStore implements RuleStore {

    private Set<Integer> rules = new HashSet<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Integer> readRules() throws RuleStoreException {
        return new HashSet<Integer>(rules);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRules(Set<Integer> newRules) throws RuleStoreException {
        rules = new HashSet<>(newRules);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() throws RuleStoreException {
        rules.clear();
    }

}