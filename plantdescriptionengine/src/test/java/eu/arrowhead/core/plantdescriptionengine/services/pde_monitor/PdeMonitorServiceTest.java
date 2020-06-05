package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import org.junit.Test;


import javax.net.ssl.SSLException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.InMemoryBackingStore;
import se.arkalix.ArSystem;
import se.arkalix.net.http.client.HttpClient;

public class PdeMonitorServiceTest {

    @Test
    public void shouldProvideService() throws BackingStoreException, SSLException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final HttpClient client = new HttpClient.Builder().build();

        final ArSystem arSystem = new ArSystem.Builder()
            .name("Test System")
            .insecure()
            .build();
        final var service = new PdeMonitorService(arSystem, entryMap, client, false);

        service.provide()
            .ifSuccess(result -> {
                assertNotNull(result);
            })
            .onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
    }

    @Test
    public void shouldNotAllowSecureService() throws BackingStoreException, SSLException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final HttpClient client = new HttpClient.Builder().build();

        final ArSystem arSystem = new ArSystem.Builder()
            .name("Test System")
            .insecure()
            .build();
        final var service = new PdeMonitorService(arSystem, entryMap, client, true);

        service.provide()
            .ifSuccess(result -> {
                assertNull(result);
            })
            .onFailure(e -> {
                assertNotNull(e);
            });
    }
}