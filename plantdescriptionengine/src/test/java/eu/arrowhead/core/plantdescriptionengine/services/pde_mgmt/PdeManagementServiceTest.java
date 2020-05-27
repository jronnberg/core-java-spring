package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.InMemoryBackingStore;
import se.arkalix.ArSystem;

public class PdeManagementServiceTest {

    @Test
    public void shouldCreateSecureService() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var service = new PdeManagementService(entryMap, true);
        assertTrue(service.isSecure());
    }

    @Test
    public void shouldCreateInsecureService() throws BackingStoreException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var service = new PdeManagementService(entryMap, false);
        assertFalse(service.isSecure());
    }

    @Test
    public void shouldProvideService() throws BackingStoreException, SSLException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var service = new PdeManagementService(entryMap, false);

        final ArSystem arSystem = new ArSystem.Builder()
            .name("Test System")
            .insecure()
            .build();
        service.provide(arSystem)
            .ifSuccess(result -> {
                assertNotNull(result);
            })
            .onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        assertFalse(service.isSecure());
    }
}