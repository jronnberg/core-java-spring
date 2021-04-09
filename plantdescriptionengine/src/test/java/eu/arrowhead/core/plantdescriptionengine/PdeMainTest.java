package eu.arrowhead.core.plantdescriptionengine;

import org.junit.jupiter.api.Test;
import se.arkalix.ArSystem;
import se.arkalix.net.http.client.HttpClient;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdeMainTest {

    @Test
    public void shouldCreateInsecureHttpClient() {

        final Properties appProps = new Properties();
        appProps.setProperty("server.ssl.enabled", "false");

        final HttpClient pdeClient = PdeMain.createHttpClient(appProps);
        assertFalse(pdeClient.isSecure());
    }

    @Test
    public void shouldCreateSecureHttpClient() {

        final ClassLoader classLoader = getClass().getClassLoader();

        final File keyStoreFile = new File(Objects.requireNonNull(classLoader.getResource("crypto/keystore.p12")).getFile());
        final String keyStorePath = keyStoreFile.getAbsolutePath();

        final int localPort = 8000;
        final Properties appProps = new Properties();
        appProps.setProperty("server.port", Integer.toString(localPort));
        appProps.setProperty("server.ssl.enabled", "true");
        appProps.setProperty("server.ssl.pde.key-store", keyStorePath);
        appProps.setProperty("server.ssl.pde.trust-store", keyStorePath);
        appProps.setProperty("server.ssl.pde.key-password", "123456");
        appProps.setProperty("server.ssl.pde.trust-store-password", "123456");
        appProps.setProperty("server.ssl.pde.key-store-password", "123456");
        final HttpClient client = PdeMain.createHttpClient(appProps);
        assertTrue(client.isSecure());
    }

    @Test
    public void shouldCreateArSystem() {
        final int localPort = 8000;
        final Properties appProps = new Properties();
        appProps.setProperty("server.port", Integer.toString(localPort));
        appProps.setProperty("server.ssl.enabled", "false");
        final InetSocketAddress address = new InetSocketAddress("0.0.0.0", 5000);
        final ArSystem arSystem = PdeMain.createArSystem(appProps, address);

        assertEquals(localPort, arSystem.port());
        assertEquals("pde", arSystem.name());
        assertFalse(arSystem.isSecure());
    }

    @Test
    public void shouldReportMissingField() {
        final int localPort = 8000;
        final Properties appProps = new Properties();
        appProps.setProperty("server.port", Integer.toString(localPort));
        appProps.setProperty("server.ssl.enabled", "true");
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> PdeMain.createArSystem(appProps, new InetSocketAddress("0.0.0.0", 5000)));
        assertEquals("Missing field 'server.ssl.pde.trust-store' in application properties.", exception.getMessage());
    }

    @Test
    public void shouldCreateSecureSystem() {

        final ClassLoader classLoader = getClass().getClassLoader();

        final File keyStoreFile = new File(Objects.requireNonNull(classLoader.getResource("crypto/keystore.p12")).getFile());
        final String keyStorePath = keyStoreFile.getAbsolutePath();

        final int localPort = 8000;
        final Properties appProps = new Properties();
        appProps.setProperty("server.port", Integer.toString(localPort));
        appProps.setProperty("server.ssl.enabled", "true");
        appProps.setProperty("server.ssl.pde.key-store", keyStorePath);
        appProps.setProperty("server.ssl.pde.trust-store", keyStorePath);
        appProps.setProperty("server.ssl.pde.key-password", "123456");
        appProps.setProperty("server.ssl.pde.trust-store-password", "123456");
        appProps.setProperty("server.ssl.pde.key-store-password", "123456");
        final ArSystem system = PdeMain.createArSystem(appProps, new InetSocketAddress("0.0.0.0", 5000));
        assertTrue(system.isSecure());
    }
}