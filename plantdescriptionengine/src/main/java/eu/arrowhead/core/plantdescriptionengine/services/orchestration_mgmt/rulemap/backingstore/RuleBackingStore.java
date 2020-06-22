package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap.backingstore;

import java.util.Map;
import java.util.Set;

/**
 * Interface for objects that read and write Orchestration rule info to
 * permanent storage.
 *
 * The PDE needs to keep track of which Orchestration rules it has created, and
 * to which Plant Description Entry each rule belongs. This information is
 * stored in memory, but it also needs to be persisted to permanent storage in
 * case the PDE is restarted. This interface defines the operations of classes
 * providing such storage.
 */
public interface RuleBackingStore {

    /**
     * @return A map describing the relationship between Plant Description
     *         Entries and Orchestration rules.
     *         The returned object is a mapping from Plant Description Entry
     *         ID:s to lists of Orchestration rules belonging to each entry.
     */
    Map<Integer, Set<Integer>> readRules() throws RuleBackingStoreException;

    /**
     * Stores an association between a set of Orchestration rule IDs and a
     * Plant Description Entry.
     *
     * @param int entryId ID of a Plant Description Entry.
     * @param int rules A set of Orchestration rule IDs.
     */
    void write(int entryId, Set<Integer> rules) throws RuleBackingStoreException;

    /**
     * Remove the all Orchestration rules for the given Plant Description Entry.
     *
     * @param id ID of the Plant Description Entry whose rules are to be
     *           removed.
     */
    void removePlantDescriptionRules(final int entryId) throws RuleBackingStoreException;

}