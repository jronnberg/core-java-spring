package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import org.junit.Test;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.pdentrymap.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.InMemoryPdStore;
import se.arkalix.security.access.AccessPolicy;

public class PdeManagementServiceTest {

    @Test
    public void shouldProvideSecureService() throws PdStoreException, SSLException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryPdStore());
        final var service = new PdeManagementService(entryMap, true).getService();
        assertEquals(AccessPolicy.cloud(), service.accessPolicy());
    }

    @Test
    public void shouldProvideInsecureService() throws PdStoreException, SSLException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryPdStore());
        final var service = new PdeManagementService(entryMap, false).getService();

        assertEquals(AccessPolicy.unrestricted(), service.accessPolicy());
    }
}