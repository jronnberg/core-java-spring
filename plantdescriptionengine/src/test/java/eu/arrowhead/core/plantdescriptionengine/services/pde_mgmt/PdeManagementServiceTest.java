package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.NullBackingStore;
import se.arkalix.security.access.AccessPolicy;

public class PdeManagementServiceTest {

    @Test
    public void shouldCreateSecureService() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        final var service = new PdeManagementService(entryMap).getService();
        assertNotNull(service);
        assertEquals(AccessPolicy.cloud(), service.accessPolicy());
    }

    @Test
    public void shouldCreateInsecureService() throws BackingStoreException {
        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        final var service = new PdeManagementService(entryMap).getService(false);
        assertNotNull(service);
        assertEquals(AccessPolicy.unrestricted(), service.accessPolicy());
    }
}