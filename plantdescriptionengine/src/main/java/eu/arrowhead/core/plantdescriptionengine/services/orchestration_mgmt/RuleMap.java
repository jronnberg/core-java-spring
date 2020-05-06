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
    List<Integer> get(int entryId) {
        return new ArrayList<Integer>(ruleLists.get(entryId));
    }

    /**
     * Stores IDs of orchestrator rules relating to a Plant Description Entry.
     *
     * @param plantDescriptionEntryId A Plant Description Entry ID.
     * @param rules A StoreEntryList object describing the Plant Description
     *              entry's orchestrator rules.
     */
	public void put(int plantDescriptionEntryId, StoreEntryList rules) {
        List<Integer> entryRules = new ArrayList<>();
        for (var storeEntry : rules.data()) {
            entryRules.add(storeEntry.id());
        }
        ruleLists.put(plantDescriptionEntryId, entryRules);
	}
}