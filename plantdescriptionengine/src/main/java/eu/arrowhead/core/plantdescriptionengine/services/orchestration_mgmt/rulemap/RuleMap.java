package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryList;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap.backingstore.RuleBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap.backingstore.RuleBackingStoreException;

/**
 * Class used for remembering which Orchestration rules the PDE has created,
 * and to which Plant Description Entry each rule belongs.
 */
public class RuleMap {

    // Non-volatile storage for rules:
    private final RuleBackingStore backingStore;

    // Internal Map for storing rules as follows:
    // <PlantDescriptionEntryID, Set<RuleId>>
    private Map<Integer, Set<Integer>> ruleLists = new ConcurrentHashMap<>();

    /**
     * Class constructor.
     *
     * @param backingStore Non-volatile storage for rules.
     * @throws RuleBackingStoreException
     */
    public RuleMap(RuleBackingStore backingStore) throws RuleBackingStoreException {
        this.backingStore = backingStore;
        ruleLists = backingStore.readRules();
    }

    /**
     * @return A mapping from Plant Description Entry ID:s to sets of
     *         Orchestrator rules belonging to that entry.
     */
    public Map<Integer, Set<Integer>> all() {
        Map<Integer, Set<Integer>> result = new HashMap<>();
        result.putAll(ruleLists);
        return result;
    }

    /**
     * @param entryId The ID of a Plant Description Entry.
     * @return A set containing the ID:s of orchestrator rules corresponding to
     *         the entry's list of connections.
     */
    public Set<Integer> get(int entryId) {
        var rules = ruleLists.get(entryId);
        if (rules == null) {
            return null;
        }
        return new HashSet<Integer>(rules);
    }

    /**
     * Stores IDs of orchestrator rules relating to a Plant Description Entry.
     *
     * @param plantDescriptionEntryId A Plant Description Entry ID.
     * @param rules                   A StoreEntryList object describing the Plant
     *                                Description entry's orchestrator rules.
     * @throws RuleBackingStoreException
     */
    public void put(final int plantDescriptionEntryId, final StoreEntryList rules) throws RuleBackingStoreException {

        Set<Integer> entryRules = new HashSet<>();
        for (var storeEntry : rules.data()) {
            entryRules.add(storeEntry.id());
        }

        backingStore.write(plantDescriptionEntryId, entryRules);
        ruleLists.put(plantDescriptionEntryId, entryRules);
	}

    /**
     * Removes all rules for a given Plant Description Entry.
     *
     * @param entryId ID of the Plant description Entry.
     * @throws RuleBackingStoreException
     */
    public void remove(final int entryId) throws RuleBackingStoreException {
        backingStore.removePlantDescriptionRules(entryId);
        ruleLists.remove(entryId);
	}
}