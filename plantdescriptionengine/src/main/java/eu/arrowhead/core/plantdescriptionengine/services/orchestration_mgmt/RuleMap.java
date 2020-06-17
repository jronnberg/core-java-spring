package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryList;

public class RuleMap {


    // Internal Map for storing rules as follows:
    // <PlantDescriptionEntryID, List<RuleId>>
    private Map<Integer, List<Integer>> ruleLists = new ConcurrentHashMap<>();

    /**
     * @param entryId The ID of a Plant Description Entry.
     * @return A list of ID:s for orchestrator rules corresponding to the
     *         entry's list of connections.
     */
    public List<Integer> get(int entryId) {
        var rules = ruleLists.get(entryId);
        if (rules == null) {
            return null;
        }
        return new ArrayList<Integer>(rules);
    }

    /**
     * Stores IDs of orchestrator rules relating to a Plant Description Entry.
     *
     * @param plantDescriptionEntryId A Plant Description Entry ID.
     * @param rules                   A StoreEntryList object describing the
     *                                Plant Description entry's orchestrator
     *                                rules.
     */
    public void put(final int plantDescriptionEntryId, final StoreEntryList rules) {
        List<Integer> entryRules = new ArrayList<>();
        for (var storeEntry : rules.data()) {
            entryRules.add(storeEntry.id());
        }
        ruleLists.put(plantDescriptionEntryId, entryRules);
	}

    /**
     * Removes all rules for a given Plant Description Entry.
     *
     * @param plantDescriptionEntryId ID of the Plant description Entry.
     */
    public void remove(final int plantDescriptionEntryId) {
        ruleLists.remove(plantDescriptionEntryId);
	}
}