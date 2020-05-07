package eu.arrowhead.core.plantdescriptionengine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Properties;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.services.management.PdeManagementService;
import eu.arrowhead.core.plantdescriptionengine.services.management.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore.BackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore.FileStore;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.OrchestratorClient;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudDto;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

public class PdeMain {

    private static final Logger logger = LoggerFactory.getLogger(PdeMain.class);

    /**
     *
     * @param identity   Holds the Arrowhead certificate chain and private key
     *                   required to manage an owned system or operator
     *                   identity.
     * @param trustStore Holds certificates associated with trusted Arrowhead
     *                   systems, operators, clouds, companies and other
     *                   authorities.
     * @return Client useful for sending HTTP messages via TCP connections to
     *         remote hosts.
     */
    private static HttpClient createHttpClient(OwnedIdentity identity, TrustStore trustStore) {
        HttpClient client = null;
        try {
            client = new HttpClient.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .build();
        } catch (SSLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return client;
    }

    /**
     * Loads an identity object.
     * The provided arguments {@code keyPassword} and {@code keyStorePassword}
     * are cleared for security reasons.
     * If this function fails, the entire application is terminated.
     *
     * @param keyStorePath Sets path to file containing JVM-compatible key
     *                     store.
     * @param keyPassword Password of private key associated with
     *                    designated certificate in key store.
     * @param keyStorePassword Password of provided key store.
     * @return An object holding the Arrowhead certificate chain and private key
     *         required to manage an owned system or operator identity.
     */
    private static OwnedIdentity loadIdentity(String keyStorePath, char[] keyPassword, char[] keyStorePassword) {
        OwnedIdentity identity = null;
        try {
            identity = new OwnedIdentity.Loader()
                .keyStorePath(keyStorePath)
                .keyPassword(keyPassword)
                .keyStorePassword(keyStorePassword)
                .load();
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Failed to load OwnedIdentity", e);
            System.exit(1);
        }

        Arrays.fill(keyPassword, '\0');
        Arrays.fill(keyStorePassword, '\0');

        return identity;
    }

    /**
     * Loads a trust store.
     * The provided argument {@code password} is cleared for security reasons.
     * If this function fails, the entire application is terminated.
     *
     * @param path Filesystem path to key store to load.
     * @param password Key store password.
     * @return Object holding certificates associated with trusted Arrowhead
     *         systems, operators, clouds, companies and other authorities.
     */
    private static TrustStore loadTrustStore(String path, char[] password) {
        TrustStore trustStore = null;
        try {
            trustStore = TrustStore.read(path, password);
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Failed to load OwnedIdentity", e);
            System.exit(1);
        }

        Arrays.fill(password, '\0');

        return trustStore;
    }

    /**
     * Main method of the Plant Description Engine.
     * Provides Plant Description management and monitoring services to the
     * Arrowhead system.
     *
     * @param args
     */
    public static void main(final String[] args) {

        Properties appProps = new Properties();
        Properties demoProps = new Properties();

        try {
            appProps.load(ClassLoader.getSystemResourceAsStream("application.properties"));
            demoProps.load(ClassLoader.getSystemResourceAsStream("demo.properties"));
        } catch (IOException ex) {
            System.out.println("Failed to read application.properties.");
            System.exit(74);
        }

        final TrustStore providerTrustStore = loadTrustStore(
            appProps.getProperty("server.ssl.provider-trust-store"),
            appProps.getProperty("server.ssl.provider-trust-store-password").toCharArray()
        );

        final OwnedIdentity providerIdentity = loadIdentity(
            appProps.getProperty("server.ssl.provider-key-store"),
            appProps.getProperty("server.ssl.provider-key-store-password").toCharArray(),
            appProps.getProperty("server.ssl.provider-key-store-password").toCharArray()
        );


        final int pdePort = Integer.parseInt(appProps.getProperty("server.port"));
        final String serviceRegistryIp = appProps.getProperty("service_registry.address");
        final int serviceRegistryPort = Integer.parseInt(appProps.getProperty("service_registry.port"));
        final var serviceRegistryAddress = new InetSocketAddress(serviceRegistryIp, serviceRegistryPort);

        final var arSystem = new ArSystem.Builder()
            .identity(providerIdentity)
            .trustStore(providerTrustStore)
            .plugins(HttpJsonCloudPlugin
                .viaServiceRegistryAt(serviceRegistryAddress))
            .localPort(pdePort)
            .build();


        // Load sysop trust store
        TrustStore consumerTrustStore = loadTrustStore(
            appProps.getProperty("server.ssl.consumer-trust-store"),
            appProps.getProperty("server.ssl.consumer-trust-store-password").toCharArray()
        );

        // Load sysop identity
        OwnedIdentity consumerIdentity = loadIdentity(
            appProps.getProperty("server.ssl.consumer-key-store"),
            appProps.getProperty("server.ssl.consumer-key-password").toCharArray(),
            appProps.getProperty("server.ssl.consumer-key-store-password").toCharArray()
        );

        final HttpClient httpClient = createHttpClient(consumerIdentity, consumerTrustStore);
        final CloudDto cloud = new CloudBuilder()
            .name(demoProps.getProperty("cloud.name"))
            .operator(demoProps.getProperty("cloud.operator"))
            .build();

        SystemTracker.initialize(httpClient, serviceRegistryAddress).ifSuccess(result -> {

            final String plantDescriptionsDirectory = appProps.getProperty("plant_descriptions");
            BackingStore entryStore = new FileStore(plantDescriptionsDirectory);
            PlantDescriptionEntryMap entryMap = null;

            try {
                entryMap = new PlantDescriptionEntryMap(entryStore);
            } catch (BackingStoreException e) {
                e.printStackTrace();
                System.exit(1);
            }

            final var orchestratorClient = new OrchestratorClient(httpClient, cloud);

            // Register the orchestrator client to Plant Description update
            // events. This will cause it to interact with the Orchestrator
            // whenever a Plant description entry is added, updated or removed.
            entryMap.addListener(orchestratorClient);

            final var pdeManager = new PdeManagementService(entryMap);

            logger.info("Providing services...");
            arSystem.provide(pdeManager.getService())
                .onFailure(Throwable::printStackTrace);
        })
        .onFailure(throwable -> {
            throwable.printStackTrace();
            System.exit(1);
        });

    }
}