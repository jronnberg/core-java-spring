package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import org.junit.Test;


import javax.net.ssl.SSLException;

import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.InMemoryBackingStore;
import se.arkalix.security.access.AccessPolicy;

public class PdeManagementServiceTest {

    @Test
    public void shouldProvideSecureService() throws BackingStoreException, SSLException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var service = new PdeManagementService(entryMap, true).getService();

        assertEquals(AccessPolicy.cloud(), service.accessPolicy());
    }

    @Test
    public void shouldProvideInsecureService() throws BackingStoreException, SSLException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var service = new PdeManagementService(entryMap, false).getService();

        assertEquals(AccessPolicy.unrestricted(), service.accessPolicy());
    }
}