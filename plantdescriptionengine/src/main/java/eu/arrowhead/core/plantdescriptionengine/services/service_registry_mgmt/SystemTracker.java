package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt;

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
 * Singleton object used to keep track of registered Arrowhead systems.
 *
 * The Singleton pattern is implemented using an Enum, as described in Joshua
 * Bloch's Effective Java.
 */
public enum SystemTracker {

    INSTANCE; // Singleton instance

    private HttpClient httpClient = null;
    private InetSocketAddress serviceRegistryAddress = null;
    private static boolean initialized;

    private Map<String, SrSystem> systems = new ConcurrentHashMap<>();

    /**
     * @return A Future which will complete with a list of registered systems.
     *
     * The retrieved systems are stored locally, and can be accessed using
     * {@link #getComponentAt(String) getSystem}.
     */
    public Future<Void> refreshSystems() {
        if (!initialized) {
            throw new IllegalStateException("SystemTracker has not been initialized.");
        }
        return httpClient.send(serviceRegistryAddress, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/serviceregistry/mgmt/systems")
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, SrSystemListDto.class))
            .flatMap(systemList -> {
                for (var system : systemList.data()) {
                    systems.put(system.systemName(), system);
                }
                return Future.done();
            });
    }

    /**
     * Sets up the global singleton instance, retrieving systems from registry.
     *
     * {@link #refreshSystems()} can be called to keep the cached list of
     * services up-to-date with the state of the Service Registry.
     *
     * @param httpClient Object for communicating with the Service Registry.
     * @param serviceRegistryAddress Address of the Service Registry.
     */
    public static Future<Void> initialize(HttpClient httpClient, InetSocketAddress serviceRegistryAddress) {
        if (initialized) {
            throw new IllegalStateException("SystemTracker has already been initialized.");
        }

        Objects.requireNonNull(serviceRegistryAddress, "Expected service registry address");
        Objects.requireNonNull(httpClient, "Expected HTTP client");

        INSTANCE.serviceRegistryAddress = serviceRegistryAddress;
        INSTANCE.httpClient = httpClient;

        initialized = true;

        return INSTANCE.refreshSystems();
    }

    /**
     * Retrieves the specified system.
     * Note that the returned data will be stale if the system in question has
     * changed state since the last call to {@link #refreshSystems()}.
     *
     *
     * @param systemName Name of a system to be retrieved.
     * @return The desired system, if it is present in the local cache.
     */
    public SrSystem getSystem(String systemName) {
        if (!initialized) {
            throw new IllegalStateException("SystemTracker has not been initialized.");
        }
        return systems.get(systemName);
    }

}