package eu.arrowhead.core.plantdescriptionengine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Properties;

import javax.net.ssl.SSLException;

import eu.arrowhead.core.plantdescriptionengine.services.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.services.management.PdeManagementService;
import eu.arrowhead.core.plantdescriptionengine.services.management.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore.BackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.management.BackingStore.FileStore;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.OrchestratorClient;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudDto;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCoreIntegrator;
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

        Properties appProps = new Properties();
        Properties demoProps = new Properties();

        try {
            appProps.load(ClassLoader.getSystemResourceAsStream("application.properties"));
            demoProps.load(ClassLoader.getSystemResourceAsStream("demo.properties"));
        } catch (IOException ex) {
            System.out.println("Failed to read application.properties.");
            System.exit(74);
        }

        final String trustStorePath = appProps.getProperty("server.ssl.trust-store");
        final char[] trustStorePassword = appProps.getProperty("server.ssl.trust-store-password").toCharArray();

        final String keyStorePath = appProps.getProperty("server.ssl.key-store");
        final char[] keyPassword = appProps.getProperty("server.ssl.key-store-password").toCharArray();
        final char[] keyStorePassword = appProps.getProperty("server.ssl.key-store-password").toCharArray();

        TrustStore trustStore = null;
        OwnedIdentity identity = null;

        try {
            trustStore = TrustStore.read(trustStorePath, trustStorePassword);
            identity = new OwnedIdentity.Loader()
                .keyPassword(keyPassword)
                .keyStorePath(keyStorePath)
                .keyStorePassword(keyStorePassword)
                .load();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Arrays.fill(keyPassword, '\0');
        Arrays.fill(keyStorePassword, '\0');
        Arrays.fill(trustStorePassword, '\0');

        final int pdePort = Integer.parseInt(appProps.getProperty("server.port"));
        final String serviceRegistryIp = appProps.getProperty("service_registry.address");
        final int serviceRegistryPort = Integer.parseInt(appProps.getProperty("service_registry.port"));
        final var serviceRegistryAddress = new InetSocketAddress(serviceRegistryIp, serviceRegistryPort);

        final var arSystem = new ArSystem.Builder()
            .identity(identity)
            .trustStore(trustStore)
            .plugins(HttpJsonCoreIntegrator
                .viaServiceRegistryAt(serviceRegistryAddress))
            .localPort(pdePort)
            .build();

        final String plantDescriptionsDirectory = appProps.getProperty("plant_descriptions");

        BackingStore entryStore = new FileStore(plantDescriptionsDirectory);
        PlantDescriptionEntryMap entryMap = null;

        try {
            entryMap = new PlantDescriptionEntryMap(entryStore);
        } catch (BackingStoreException e) {
            e.printStackTrace();
            System.exit(1);
        }

        HttpClient httpClient = null;

        try {
            httpClient = new HttpClient.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .build();
        } catch (SSLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        final String cloudName = demoProps.getProperty("cloud.name");
        final String cloudOperator = demoProps.getProperty("cloud.operator");
        final CloudDto cloud = new CloudBuilder().name(cloudName).operator(cloudOperator).build();

        final String orchestratorAddress = demoProps.getProperty("orchestrator.address");
        final int orchestratorPort = Integer.parseInt(demoProps.getProperty("orchestrator.port"));
        final var orchestratorClient = new OrchestratorClient(httpClient, orchestratorAddress, orchestratorPort, cloud);

        final var pdeManager = new PdeManagementService(entryMap, orchestratorClient);

        SystemTracker.initialize(serviceRegistryAddress, httpClient).ifSuccess(result -> {
            System.out.println("Providing services...");
            arSystem.provide(pdeManager.getService())
                .onFailure(Throwable::printStackTrace);
        })
        .onFailure(throwable -> {
            throwable.printStackTrace();
            System.exit(1);
        });

    }
}