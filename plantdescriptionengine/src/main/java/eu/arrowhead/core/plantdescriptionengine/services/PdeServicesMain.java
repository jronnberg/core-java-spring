package eu.arrowhead.core.plantdescriptionengine.services;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

import eu.arrowhead.core.plantdescriptionengine.services.management.PdeManagementService;
import se.arkalix.ArSystem;
import se.arkalix.security.X509KeyStore;
import se.arkalix.security.X509TrustStore;

public class PdeServicesMain {

    // File path to the directory for storing JSON representations of plant
    // descriptions:
    final static String DESCRIPTION_DIRECTORY = "plant-descriptions/";

    /**
     * @param password Password of the private key associated with the
     *                 certificate in key store.
     * @param keyStorePath Path to the keystore representing the systems own
     *                     identity.
     * @param trustStorePath Path to the trust store representing all identities
     *                       that are to be trusted by the system.
     * @return An Arrowhead Framework system.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private static ArSystem initArSystem(final char[] password, final String keyStorePath, final String trustStorePath)
            throws GeneralSecurityException, IOException {

        X509KeyStore keyStore = null;
        X509TrustStore trustStore = null;

        keyStore = new X509KeyStore.Loader()
            .keyPassword(password)
            .keyStorePath(Path.of(keyStorePath))
            .keyStorePassword(password).load();
        trustStore = X509TrustStore.read(Path.of(trustStorePath), password);

        final var system = new ArSystem.Builder()
            .keyStore(keyStore)
            .trustStore(trustStore)
            .localPort(28081)
            .build();

        return system;
    }

    public static void main(final String[] args) {

        if (args.length != 2) {
            System.err.println("Requires two command line arguments: <keyStorePath> and <trustStorePath>");
            System.exit(1);
        }

        final var password = new char[] { '1', '2', '3', '4', '5', '6' };
        ArSystem arSystem = null;

        try {
            arSystem = initArSystem(password, args[0], args[1]);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(74);
        }

        PdeManagementService pdeManager = new PdeManagementService(DESCRIPTION_DIRECTORY);

        System.out.println("Providing services...");
        arSystem.provide(pdeManager.getService())
            .onFailure(Throwable::printStackTrace);
    }
}
