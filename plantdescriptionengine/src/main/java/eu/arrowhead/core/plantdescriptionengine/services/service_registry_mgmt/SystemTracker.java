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
 * Object used to keep track of registered Arrowhead systems.
 */
public class SystemTracker {

    private final HttpClient httpClient;
    private InetSocketAddress serviceRegistryAddress = null;
    private boolean initialized;

    // Map from system name to system:
    private Map<String, SrSystem> systems = new ConcurrentHashMap<>();

    /**
     * Class constructor
     *
     * @param httpClient Object for communicating with the Service Registry.
     * @param serviceRegistryAddress Address of the Service Registry.
     *
     */
    public SystemTracker(final HttpClient httpClient, final InetSocketAddress serviceRegistryAddress) {

        Objects.requireNonNull(serviceRegistryAddress, "Expected service registry address");
        Objects.requireNonNull(httpClient, "Expected HTTP client");

        this.httpClient = httpClient;
        this.serviceRegistryAddress = serviceRegistryAddress;
    }

    /**
     * @return A Future which will complete with a list of registered systems.
     *
     * The retrieved systems are stored locally, and can be accessed using
     * {@link #getComponentAt(String) getSystem}.
     */
    public Future<Void> refreshSystems() {
        return httpClient.send(serviceRegistryAddress, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/serviceregistry/mgmt/systems")
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, SrSystemListDto.class))
            .flatMap(systemList -> {
                for (var system : systemList.data()) {
                    systems.put(system.systemName(), system);
                }
                initialized = true;
                return Future.done();
            });
    }

    /**
     * Retrieves the specified system.
     * Note that the returned data will be stale if the system in question has
     * changed state since the last call to {@link #refreshSystems()}.
     *
     *
     * @param systemName Name of a system.
     * @return The desired system, if it is present in the local cache.
     */
    public SrSystem getSystemByName(String systemName) {
        if (!initialized) {
            throw new IllegalStateException("SystemTracker has not been initialized.");
        }
        return systems.get(systemName);
    }

    /**
     * Retrieves the specified system.
     * Note that the returned data will be stale if the system in question has
     * changed state since the last call to {@link #refreshSystems()}.
     *
     *
     * @param systemName Name of a system.
     * @return The desired system, if it is present in the local cache.
     */
    public SrSystem getSystem(String systemName) {
        if (!initialized) {
            throw new IllegalStateException("SystemTracker has not been initialized.");
        }
        return systems.get(systemName);
    }

}