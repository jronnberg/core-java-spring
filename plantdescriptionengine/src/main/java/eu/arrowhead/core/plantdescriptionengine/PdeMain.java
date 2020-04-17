package eu.arrowhead.core.plantdescriptionengine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import eu.arrowhead.core.plantdescriptionengine.services.management.PdeManagementService;
import eu.arrowhead.core.plantdescriptionengine.services.management.PlantDescriptionEntryStore;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCoreIntegrator;
import se.arkalix.dto.DtoReadException;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

public class PdeMain {

    // File path to the directory for storing JSON representations of plant
    // descriptions:
    final static String DESCRIPTION_DIRECTORY = "plant-descriptions/";
    final static String SERVICE_REGISTRY_ADDRESS = "172.39.9.15";
    final static int SERVICE_REGISTRY_PORT = 39915;

    /**
     * @param password       Password of the private key associated with the
     *                       certificate in key store.
     * @param keyStorePath   Path to the keystore representing the systems own
     *                       identity.
     * @param trustStorePath Path to the trust store representing all identities
     *                       that are to be trusted by the system.
     * @return An Arrowhead Framework system.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static ArSystem initArSystem(final char[] password, final String keyStorePath, final String trustStorePath, final int port)
            throws GeneralSecurityException, IOException {

        final var identity = new OwnedIdentity.Loader()
            .keyPassword(password)
            .keyStorePath(Path.of(keyStorePath))
            .keyStorePassword(password)
            .load();
        final var trustStore = TrustStore.read(Path.of(trustStorePath), password);
        Arrays.fill(password, '\0');

        final var serviceRegistryAddress = new InetSocketAddress(SERVICE_REGISTRY_ADDRESS, SERVICE_REGISTRY_PORT);

        final var system = new ArSystem.Builder()
            .name("PDE-demo")
            .insecure()
            .localPort(port)
            .build();

        // final var system = new ArSystem.Builder()
        //     .identity(identity)
        //     .trustStore(trustStore)
        //     .plugins(HttpJsonCoreIntegrator.viaServiceRegistryAt(serviceRegistryAddress))
        //     .localPort(PORT)
        //     .build();

        return system;
    }

    /**
     * Main method of the Plant Description Engine.
     * Provides Plant Description management and monitoring services to the
     * Arrowhead system.
     * @param args
     */
    public static void main(final String[] args) {

        if (args.length != 3) {
            System.err.println("Requires three command line arguments: KeyStorePath, trustStorePath and port.");
            System.exit(1);
        }

        final var password = new char[] { '1', '2', '3', '4', '5', '6' };
        ArSystem arSystem = null;
        final int port = Integer.parseInt(args[2]);

        try {
            arSystem = initArSystem(password, args[0], args[1], port);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(74);
        }

        var entryStore = new PlantDescriptionEntryStore(DESCRIPTION_DIRECTORY);
        try {
            // Read Plant Description entries from file.
            entryStore.readEntries();

        } catch (IOException | DtoReadException e) {
            e.printStackTrace();
            System.exit(74);
        }
        var pdeManager = new PdeManagementService(entryStore);

        System.out.println("Providing services...");
        arSystem.provide(pdeManager.getService())
            .onFailure(Throwable::printStackTrace);
    }
}
