package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import se.arkalix.ArSystem;
import se.arkalix.net.http.client.HttpClient;

public class PdeMonitorServiceTest {

    @Test
    public void shouldProvideService() throws PdStoreException, SSLException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final HttpClient client = new HttpClient.Builder().build();

        final ArSystem arSystem = new ArSystem.Builder()
            .name("Test System")
            .insecure()
            .build();
        final var service = new PdeMonitorService(arSystem, pdTracker, client, new AlarmManager(), false);

        service.provide()
            .ifSuccess(result -> {
                assertNotNull(result);
            })
            .onFailure(e -> {
                assertNull(e);
            });
    }

    @Test
    public void shouldNotAllowSecureService() throws PdStoreException, SSLException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final HttpClient client = new HttpClient.Builder().build();

        final ArSystem arSystem = new ArSystem.Builder()
            .name("Test System")
            .insecure()
            .build();
        final var service = new PdeMonitorService(arSystem, pdTracker, client, new AlarmManager(), true);

        service.provide()
            .ifSuccess(result -> {
                assertNull(result);
            })
            .onFailure(e -> {
                assertNotNull(e);
            });
    }
}