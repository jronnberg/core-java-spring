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
     * @param appProps Configurations used for this instance of the Plant
     *                 Description Engine.
     * @return HTTP client useful for sending messages to remote hosts.
     */
    private static HttpClient createHttpClient(Properties appProps) {
        final boolean secureMode = Boolean.parseBoolean(appProps.getProperty("server.ssl.enabled"));
        HttpClient client = null;
        try {
            if (!secureMode) {
                client = new HttpClient.Builder().insecure().build();
            } else {
                OwnedIdentity identity = loadIdentity(
                    appProps.getProperty("server.ssl.consumer-key-store"),
                    appProps.getProperty("server.ssl.consumer-key-password").toCharArray(),
                    appProps.getProperty("server.ssl.consumer-key-store-password").toCharArray()
                );

                TrustStore trustStore = loadTrustStore(
                    appProps.getProperty("server.ssl.consumer-trust-store"),
                    appProps.getProperty("server.ssl.consumer-trust-store-password").toCharArray()
                );

                client = new HttpClient.Builder()
                    .identity(identity)
                    .trustStore(trustStore)
                    .build();
            }
        } catch (SSLException e) {
            logger.error("Failed to create HTTP Client", e);
            System.exit(1);
        }
        return client;
    }

    /**
     * @param appProps Configurations used for this instance of the Plant
     *                 Description Engine.
     * @param serviceRegistryAddress Address of the Service Registry.
     * @return An Arrowhead Framework System.
     */
    private static ArSystem createArSystem(Properties appProps, InetSocketAddress serviceRegistryAddress) {

        final int pdePort = Integer.parseInt(appProps.getProperty("server.port"));
        final ArSystem.Builder systemBuilder = new ArSystem.Builder()
            .localPort(pdePort);

        final boolean secureMode = Boolean.parseBoolean(appProps.getProperty("server.ssl.enabled"));

        if (!secureMode) {
            systemBuilder.name("pde").insecure(); // TODO: Use some other name?
        } else {
            final String trustStorePath = appProps.getProperty("server.ssl.provider-trust-store");
            final char[] trustStorePassword = appProps.getProperty("server.ssl.provider-trust-store-password").toCharArray();
            final String keyStorePath = appProps.getProperty("server.ssl.provider-key-store");
            final char[] keyPassword = appProps.getProperty("server.ssl.provider-key-password").toCharArray();
            final char[] keyStorePassword = appProps.getProperty("server.ssl.provider-key-store-password").toCharArray();

            systemBuilder
                .identity(loadIdentity(keyStorePath, keyPassword, keyStorePassword))
                .trustStore(loadTrustStore(trustStorePath, trustStorePassword))
                .plugins(HttpJsonCloudPlugin // TODO: This should be done in insecure mode as well.
                    .viaServiceRegistryAt(serviceRegistryAddress));

        }

        return systemBuilder.build();

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

        final String serviceRegistryIp = appProps.getProperty("service_registry.address");
        final int serviceRegistryPort = Integer.parseInt(appProps.getProperty("service_registry.port"));
        final var serviceRegistryAddress = new InetSocketAddress(serviceRegistryIp, serviceRegistryPort);

        final ArSystem arSystem = createArSystem(appProps, serviceRegistryAddress);
        final HttpClient httpClient = createHttpClient(appProps);

        SystemTracker.initialize(httpClient, serviceRegistryAddress).flatMap(result -> {

            final String plantDescriptionsDirectory = appProps.getProperty("plant_descriptions");
            final BackingStore entryStore = new FileStore(plantDescriptionsDirectory);
            final var entryMap = new PlantDescriptionEntryMap(entryStore);
            final CloudDto cloud = new CloudBuilder()
                .name(demoProps.getProperty("cloud.name"))
                .operator(demoProps.getProperty("cloud.operator"))
                .build();
            final var orchestratorClient = new OrchestratorClient(httpClient, cloud);

            // Register the Orchestrator client to Plant Description events.
            // This will cause it to interact with the Orchestrator whenever a
            // Plant Description Entry is added, updated or removed.
            entryMap.addListener(orchestratorClient);

            final var pdeManager = new PdeManagementService(entryMap);
            final boolean secureMode = Boolean.parseBoolean(appProps.getProperty("server.ssl.enabled"));

            logger.info("Providing services...");
            return arSystem.provide(pdeManager.getService(secureMode));
        })
        .onFailure(throwable -> {
            logger.error("Failed to launch Plant Description Engine", throwable);
            System.exit(1);
        });

    }
}