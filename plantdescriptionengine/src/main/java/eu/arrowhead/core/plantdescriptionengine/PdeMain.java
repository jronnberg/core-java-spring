package eu.arrowhead.core.plantdescriptionengine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Properties;

import javax.net.ssl.SSLException;

import eu.arrowhead.core.plantdescriptionengine.services.management.PdeManagementService;
import eu.arrowhead.core.plantdescriptionengine.services.management.PlantDescriptionEntryStore;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.OrchestratorClient;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudDto;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCoreIntegrator;
import se.arkalix.dto.DtoReadException;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

public class PdeMain {
    /**
     * Main method of the Plant Description Engine.
     * Provides Plant Description management and monitoring services to the
     * Arrowhead system.
     * @param args
     */
    public static void main(final String[] args) {

        Properties prop = new Properties();

        try {
            InputStream inputStream = ClassLoader.getSystemResourceAsStream("application.properties");
            prop.load(inputStream);
        } catch (IOException ex) {
            System.out.println("Failed to read application.properties.");
            System.exit(74);
        }

        TrustStore trustStore = null;
        OwnedIdentity identity = null;

        final String trustStorePath = prop.getProperty("server.ssl.trust-store");
        final char[] trustStorePassword = prop.getProperty("server.ssl.trust-store-password").toCharArray();

        final String keyStorePath = prop.getProperty("server.ssl.key-store");
        final char[] keyPassword = prop.getProperty("server.ssl.key-store-password").toCharArray();
        final char[] keyStorePassword = prop.getProperty("server.ssl.key-store-password").toCharArray();

        try {
            trustStore = TrustStore.read(trustStorePath, trustStorePassword);
            identity = new OwnedIdentity.Loader()
                .keyPassword(keyPassword)
                .keyStorePath(keyStorePath)
                .keyStorePassword(keyStorePassword)
                .load();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(74);
        }

        Arrays.fill(keyPassword, '\0');
        Arrays.fill(keyStorePassword, '\0');
        Arrays.fill(trustStorePassword, '\0');

        final int pdePort = Integer.parseInt(prop.getProperty("server.port"));
        final String serviceRegistryAddress = prop.getProperty("service_registry.address");
        final int serviceRegistryPort = Integer.parseInt(prop.getProperty("service_registry.port"));

        final var arSystem = new ArSystem.Builder()
            .identity(identity)
            .trustStore(trustStore)
            .plugins(HttpJsonCoreIntegrator
                .viaServiceRegistryAt(new InetSocketAddress(serviceRegistryAddress, serviceRegistryPort)))
            .localPort(pdePort)
            .build();

        final String plantDescriptionsDirectory = prop.getProperty("plant_descriptions");
        var entryStore = new PlantDescriptionEntryStore(plantDescriptionsDirectory);

        try {
            // Read Plant Description entries from file.
            entryStore.readEntries();
        } catch (IOException | DtoReadException e) {
            e.printStackTrace();
            System.exit(74);
        }

        HttpClient client = null;

        try {
            client = new HttpClient.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .build();
        } catch (SSLException e) {
            System.exit(74);
            e.printStackTrace();
        }

        final CloudDto cloud = new CloudBuilder().name("xarepo").operator("xarepo").build(); // TODO: Remove hardcoded cloud
        final var orchestratorClient = new OrchestratorClient(client, cloud);
        final var pdeManager = new PdeManagementService(entryStore, orchestratorClient);

        System.out.println("Providing services...");
        arSystem.provide(pdeManager.getService())
            .onFailure(Throwable::printStackTrace);

    }
}