package eu.arrowhead.core.plantdescriptionengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

import java.util.Properties;

import se.arkalix.net.http.client.HttpClient;


public class PdeMainTest {

    @Test
    public void shouldCreateInsecureSysopHttpClient() {

        Properties appProps = new Properties();
        appProps.setProperty("server.ssl.enabled", "false");

        final HttpClient sysopClient = PdeMain.createSysopHttpClient(appProps);
        assertFalse(sysopClient.isSecure());
    }

    @Test
    public void shouldCreateInsecurePdeHttpClient() {

        Properties appProps = new Properties();
        appProps.setProperty("server.ssl.enabled", "false");

        final HttpClient pdeClient = PdeMain.createPdeHttpClient(appProps);
        assertFalse(pdeClient.isSecure());
    }

}