package eu.arrowhead.core.plantdescriptionengine.services;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystem;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystemListDto;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.util.concurrent.Future;

/**
 * TODO: Currently only has static methods, this will be changed in the future.
 */
public class SystemTracker {

    private static HttpClient httpClient = null;
    private static InetSocketAddress serviceRegistryAddress = null;
    private static boolean initialized = false;

    private static Map<String, SrSystem> systems = new ConcurrentHashMap<>();

    private SystemTracker() {}

    private static Future<Void> retrieveSystems() {
        return httpClient.send(serviceRegistryAddress, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/serviceregistry/mgmt/systems")
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, SrSystemListDto.class))
            .map(systemList -> {
                for (var system : systemList.data()) {
                    systems.put(system.systemName(), system);
                }
                return systemList;
            })
            .flatMap(result -> {
                initialized = true;
                return Future.done();
            });
    }

    public static Future<Void> initialize(InetSocketAddress aServiceRegistryAddress, HttpClient aHttpClient) {
        if (initialized) {
            throw new IllegalStateException("SystemTracker has already been initialized.");
        }

        Objects.requireNonNull(aServiceRegistryAddress, "Expected service registry address");
        Objects.requireNonNull(aHttpClient, "Expected HTTP client");

        serviceRegistryAddress = aServiceRegistryAddress;
        httpClient = aHttpClient;

        System.out.println("Attempting to retrieve systems");

        return retrieveSystems()
            .flatMap(result -> {
                initialized = true;
                return Future.done();
            });
    }

    public static SrSystem get(String systemName) {
        if (!initialized) {
            throw new IllegalStateException("SystemtTracker has not been initialized.");
        }
        return systems.get(systemName);
    }

}