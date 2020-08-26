package eu.arrowhead.core.plantdescriptionengine;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.PdeManagementService;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.FilePdStore;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.PdeMonitorService;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.OrchestratorClient;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.FileRuleStore;
import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.CloudBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.CloudDto;
import se.arkalix.ArServiceCache;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.or.OrchestrationOption;
import se.arkalix.core.plugin.or.OrchestrationPattern;
import se.arkalix.core.plugin.or.OrchestrationStrategy;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

public class PdeMain {

    private static final Logger logger = LoggerFactory.getLogger(PdeMain.class);

    /**
     * @param appProps Configurations used for this instance of the Plant
     *                 Description Engine.
     * @return HTTP client useful for consuming Arrowhead services as a
     *         privileged system.
     */
    static HttpClient createSysopHttpClient(Properties appProps) {
        final boolean secureMode = Boolean.parseBoolean(appProps.getProperty("server.ssl.enabled"));
        HttpClient client = null;
        try {
            if (!secureMode) {
                client = new HttpClient.Builder().insecure().build();
            } else {
                OwnedIdentity identity = loadIdentity(
                    appProps.getProperty("server.ssl.sysop.key-store"),
                    appProps.getProperty("server.ssl.sysop.key-password").toCharArray(),
                    appProps.getProperty("server.ssl.sysop.key-store-password").toCharArray()
                );

                TrustStore trustStore = loadTrustStore(
                    appProps.getProperty("server.ssl.sysop.trust-store"),
                    appProps.getProperty("server.ssl.sysop.trust-store-password").toCharArray()
                );

                client = new HttpClient.Builder()
                    .identity(identity)
                    .trustStore(trustStore)
                    .build();
            }
        } catch (SSLException e) {
            logger.error("Failed to create Sysop HTTP Client", e);
            System.exit(1);
        }
        return client;
    }

    /**
     * @param appProps Configurations used for this instance of the Plant
     *                 Description Engine.
     * @return HTTP client useful for consuming Arrowhead services as a
     *         non-privileged system.
     */
    static HttpClient createPdeHttpClient(Properties appProps) {
        final boolean secureMode = Boolean.parseBoolean(appProps.getProperty("server.ssl.enabled"));
        HttpClient client = null;
        try {
            if (!secureMode) {
                client = new HttpClient.Builder().insecure().build();
            } else {
                OwnedIdentity identity = loadIdentity(
                    appProps.getProperty("server.ssl.pde.key-store"),
                    appProps.getProperty("server.ssl.pde.key-password").toCharArray(),
                    appProps.getProperty("server.ssl.pde.key-store-password").toCharArray()
                );

                TrustStore trustStore = loadTrustStore(
                    appProps.getProperty("server.ssl.pde.trust-store"),
                    appProps.getProperty("server.ssl.pde.trust-store-password").toCharArray()
                );

                client = new HttpClient.Builder()
                    .identity(identity)
                    .trustStore(trustStore)
                    .build();
            }
        } catch (SSLException e) {
            logger.error("Failed to create PDE HTTP Client", e);
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

        final var strategy = new OrchestrationStrategy(
            new OrchestrationPattern()
                .isIncludingService(true)
                .option(OrchestrationOption.METADATA_SEARCH, false)
                .option(OrchestrationOption.PING_PROVIDERS, true)
                .option(OrchestrationOption.OVERRIDE_STORE, false));

        final ArSystem.Builder systemBuilder = new ArSystem.Builder()
            .serviceCache(ArServiceCache.withEntryLifetimeLimit(Duration.ZERO))
            .localPort(pdePort)
            .plugins(new HttpJsonCloudPlugin.Builder()
                .orchestrationStrategy(strategy)
                .serviceRegistrySocketAddress(serviceRegistryAddress)
                .build());

        final boolean secureMode = Boolean.parseBoolean(appProps.getProperty("server.ssl.enabled"));

        if (!secureMode) {
            systemBuilder.name("pde").insecure();
        } else {
            final String trustStorePath = appProps.getProperty("server.ssl.pde.trust-store");
            final char[] trustStorePassword = appProps.getProperty("server.ssl.pde.trust-store-password").toCharArray();
            final String keyStorePath = appProps.getProperty("server.ssl.pde.key-store");
            final char[] keyPassword = appProps.getProperty("server.ssl.pde.key-password").toCharArray();
            final char[] keyStorePassword = appProps.getProperty("server.ssl.pde.key-store-password").toCharArray();

            systemBuilder
                .identity(loadIdentity(keyStorePath, keyPassword, keyStorePassword))
                .trustStore(loadTrustStore(trustStorePath, trustStorePassword));
        }

        return systemBuilder.build();

    }

    /**
     * Loads an identity object.
     *
     * The provided arguments {@code keyPassword} and {@code keyStorePassword}
     * are cleared for security reasons.
     * If this function fails, the entire application is terminated.
     *
     * @param keyStorePath Sets path to file containing JVM-compatible key
     *                     store.
     * @param keyPassword  Password of private key associated with
     *                     designated certificate in key store.
     * @param keyStorePassword Password of provided key store.
     *
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
     *
     * The provided argument {@code password} is cleared for security reasons.
     * If this function fails, the entire application is terminated.
     *
     * @param path Filesystem path to key store to load.
     * @param password Key store password.
     *
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
     *
     * Provides Plant Description management and monitoring services to the
     * Arrowhead system.
     */
    public static void main(final String[] args) {

        Properties appProps = new Properties();

        try {
            if (args.length == 1) {
                // If a command line argument is given, interpret it as the file
                // path to an application properties file:
                appProps.load(new FileInputStream(args[0]));
            } else {
                // Otherwise, load it from app resources:
                appProps.load(ClassLoader.getSystemResourceAsStream("application.properties"));
            }

        } catch (IOException e) {
            logger.error("Failed to read application.properties.", e);
            System.exit(74);
        }

        final String serviceRegistryIp = appProps.getProperty("service_registry.address");
        final int serviceRegistryPort = Integer.parseInt(appProps.getProperty("service_registry.port"));
        final var serviceRegistryAddress = new InetSocketAddress(serviceRegistryIp, serviceRegistryPort);

        final HttpClient sysopClient = createSysopHttpClient(appProps);

        final SystemTracker systemTracker = new SystemTracker(sysopClient, serviceRegistryAddress);

        // Make the system tracker globally accessible:
        Locator.setSystemTracker(systemTracker);
        systemTracker.startPollingForSystems().flatMap(result -> {

            // Create a globally accessible alarm manager:
            Locator.setAlarmManager(new AlarmManager());

            final CloudDto cloud = new CloudBuilder()
                .name(appProps.getProperty("cloud.name"))
                .operator(appProps.getProperty("cloud.operator"))
                .build();

            final HttpClient pdeClient = createPdeHttpClient(appProps);
            final String ruleDirectory = appProps.getProperty("orchestration_rules");
            final var orchestratorClient = new OrchestratorClient(sysopClient, cloud, new FileRuleStore(ruleDirectory));
            final String plantDescriptionsDirectory = appProps.getProperty("plant_descriptions");

            final var pdTracker = new PlantDescriptionTracker(new FilePdStore(plantDescriptionsDirectory));
            final ArSystem arSystem = createArSystem(appProps, serviceRegistryAddress);
            final boolean secureMode = Boolean.parseBoolean(appProps.getProperty("server.ssl.enabled"));


            return orchestratorClient.initialize(pdTracker)
                .flatMap(orchstratorInitializationResult -> {
                    final var mismatchDetector = new SystemMismatchDetector(pdTracker);
                    mismatchDetector.run();
                    var pdeManagementService = new PdeManagementService(pdTracker, secureMode);
                    return arSystem.provide(pdeManagementService.getService());
                })
                .flatMap(mgmtServiceResult -> {
                    return new PdeMonitorService(arSystem, pdTracker, pdeClient, secureMode).provide();
                });
        })
        .onFailure(throwable -> {
            logger.error("Failed to launch Plant Description Engine", throwable);
            System.exit(1);
        });

    }
}