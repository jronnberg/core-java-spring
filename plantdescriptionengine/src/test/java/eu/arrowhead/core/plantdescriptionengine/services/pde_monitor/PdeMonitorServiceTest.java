package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import org.junit.Test;

import alarmmanager.AlarmManager;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import eu.arrowhead.core.plantdescriptionengine.pdentrymap.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.InMemoryPdStore;
import se.arkalix.ArSystem;
import se.arkalix.net.http.client.HttpClient;

public class PdeMonitorServiceTest {

    @Test
    public void shouldProvideService() throws PdStoreException, SSLException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryPdStore());
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
    public void shouldNotAllowSecureService() throws PdStoreException, SSLException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryPdStore());
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