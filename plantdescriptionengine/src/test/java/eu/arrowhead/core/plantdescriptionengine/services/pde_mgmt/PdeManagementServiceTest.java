package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import org.junit.Test;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import se.arkalix.security.access.AccessPolicy;

public class PdeManagementServiceTest {

    @Test
    public void shouldProvideSecureService() throws PdStoreException, SSLException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var service = new PdeManagementService(pdTracker, true).getService();
        assertEquals(AccessPolicy.cloud(), service.accessPolicy());
    }

    @Test
    public void shouldProvideInsecureService() throws PdStoreException, SSLException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var service = new PdeManagementService(pdTracker, false).getService();

        assertEquals(AccessPolicy.unrestricted(), service.accessPolicy());
    }
}