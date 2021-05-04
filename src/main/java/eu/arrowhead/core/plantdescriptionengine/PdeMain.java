package eu.arrowhead.core.plantdescriptionengine;

import se.arkalix.ArServiceRecordCache;
import se.arkalix.ArSystem;
import se.arkalix.codec.CodecType;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.or.OrchestrationOption;
import se.arkalix.core.plugin.or.OrchestrationPattern;
import se.arkalix.core.plugin.or.OrchestrationStrategy;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Properties;

public final class PdeMain {

    private static String getProp(final Properties appProps, final String propName) {
        final String result = appProps.getProperty(propName);
        if (result == null) {
            throw new IllegalArgumentException("Missing field '" + propName + "' in application properties.");
        }
        return result;
    }

    static ArSystem createArSystem(final Properties appProps, final InetSocketAddress serviceRegistryAddress) {

        final OrchestrationStrategy strategy = new OrchestrationStrategy(
            new OrchestrationPattern().isIncludingService(true)
                .option(OrchestrationOption.METADATA_SEARCH, false)
                .option(OrchestrationOption.PING_PROVIDERS, true)
                .option(OrchestrationOption.OVERRIDE_STORE, false));

        final int port = Integer.parseInt(getProp(appProps, "server.port"));

        return new ArSystem.Builder()
            .serviceCache(ArServiceRecordCache.withEntryLifetimeLimit(Duration.ZERO))
            .localPort(port)
            .plugins(new HttpJsonCloudPlugin.Builder()
                .orchestrationStrategy(strategy)
                .serviceRegistrySocketAddress(serviceRegistryAddress)
                .build())
            .name("pde")
            .insecure()
            .build();
    }

    private static HttpService createService() {
        return new HttpService()
            .name("pde-mgmt")
            .codecs(CodecType.JSON)
            .basePath("/pde/mgmt")
            .accessPolicy(AccessPolicy.unrestricted())
            .get("/greeting", (request, response) -> {
                response.status(HttpStatus.OK)
                    .body("{\"text\":\"Hello, Arrowhead!\"}");
                return Future.done();
            });
    }

    public static void main(final String[] args) throws IOException {

        final Properties appProps = new Properties();
        appProps.load(ClassLoader.getSystemResourceAsStream("application.properties"));

        final String serviceRegistryIp = getProp(appProps, "service_registry.address");
        final int serviceRegistryPort = Integer.parseInt(getProp(appProps, "service_registry.port"));
        final InetSocketAddress serviceRegistryAddress = new InetSocketAddress(serviceRegistryIp, serviceRegistryPort);

        final ArSystem arSystem = createArSystem(appProps, serviceRegistryAddress);
        final HttpService service = createService();

        arSystem.provide(service)
            .ifSuccess(serviceHandle -> System.out.println("Success!"))
            .onFailure(e -> {
                System.out.println("Failed to launch Plant Description Engine");
                e.printStackTrace();
                System.exit(1);
            });
    }

}