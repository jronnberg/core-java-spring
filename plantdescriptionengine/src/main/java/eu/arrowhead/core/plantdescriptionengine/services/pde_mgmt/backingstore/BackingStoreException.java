package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore;

/**
 * Signifies the failure for a BackingStore instance to communicate with its
 * underlying storage.
 */
public class BackingStoreException extends Exception {
    private static final long serialVersionUID = 2925941409432190728L;

    public BackingStoreException(String errorMessage) {
        super(errorMessage);
    }

    public BackingStoreException(Throwable throwable) {
        super(throwable);
    }

    public BackingStoreException(String errorMessage, Throwable throwable) {
        super(errorMessage, throwable);
    }
}