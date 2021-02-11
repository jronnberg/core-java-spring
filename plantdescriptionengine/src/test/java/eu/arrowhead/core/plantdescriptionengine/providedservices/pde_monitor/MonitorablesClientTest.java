package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor;

import org.junit.jupiter.api.Test;

import eu.arrowhead.core.plantdescriptionengine.MonitorInfo;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import se.arkalix.ArSystem;
import se.arkalix.net.http.client.HttpClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.net.ssl.SSLException;

public class MonitorablesClientTest {

    @Test
    public void shouldCreateClient() throws SSLException {
        final HttpClient httpClient = new HttpClient.Builder().build();

        final ArSystem arSystem = new ArSystem.Builder()
            .name("Test System")
            .insecure()
            .build();

        MonitorablesClient monClient = new MonitorablesClient(
            arSystem,
            httpClient,
            new MonitorInfo(),
            new AlarmManager()
        );

        assertNotNull(monClient);
    }

    // TODO: Find out a nice way of testing this class.

}