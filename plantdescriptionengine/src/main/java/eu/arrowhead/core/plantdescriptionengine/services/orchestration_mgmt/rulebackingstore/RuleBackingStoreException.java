package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulebackingstore;

/**
 * Signifies the failure for a BackingStore instance to communicate with its
 * underlying storage.
 */
public class RuleBackingStoreException extends Exception {
    private static final long serialVersionUID = 2925941409432190728L;

    public RuleBackingStoreException(String errorMessage) {
        super(errorMessage);
    }

    public RuleBackingStoreException(Throwable throwable) {
        super(throwable);
    }

    public RuleBackingStoreException(String errorMessage, Throwable throwable) {
        super(errorMessage, throwable);
    }
}