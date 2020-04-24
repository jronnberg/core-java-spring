package eu.arrowhead.core.plantdescriptionengine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;

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

    // File path to the directory for storing JSON representations of plant
    // descriptions:
    final static String DESCRIPTION_DIRECTORY = "plant-descriptions/";
    final static int PORT = 28081;
    final static String SERVICE_REGISTRY_ADDRESS = "127.0.0.1";
    final static int SERVICE_REGISTRY_PORT = 8443;

    /**
     * Main method of the Plant Description Engine.
     * Provides Plant Description management and monitoring services to the
     * Arrowhead system.
     * @param args
     */
    public static void main(final String[] args) {

        if (args.length != 2) {
            System.err.println("Requires two command line arguments: <keyStorePath> and <trustStorePath>");
            System.exit(1);
        }

        final var password = new char[] { '1', '2', '3', '4', '5', '6' };

        TrustStore trustStore = null;
        OwnedIdentity identity = null;

        try {
            trustStore = TrustStore.read(Path.of(args[1]), password);
            identity = new OwnedIdentity.Loader()
                .keyPassword(password)
                .keyStorePath(Path.of(args[0]))
                .keyStorePassword(password)
                .load();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(74);
        }

        Arrays.fill(password, '\0');

        final var arSystem = new ArSystem.Builder()
            .identity(identity)
            .trustStore(trustStore)
            .plugins(HttpJsonCoreIntegrator
                .viaServiceRegistryAt(new InetSocketAddress(SERVICE_REGISTRY_ADDRESS, SERVICE_REGISTRY_PORT)))
            .localPort(PORT)
            .build();

        var entryStore = new PlantDescriptionEntryStore(DESCRIPTION_DIRECTORY);
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