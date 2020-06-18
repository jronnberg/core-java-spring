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

    private Map<Integer, SrSystem> systemsById = new ConcurrentHashMap<>();
    private Map<String, SrSystem> systemsByName = new ConcurrentHashMap<>();

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
                    systemsById.put(system.id(), system);
                    systemsByName.put(system.systemName(), system);
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
     * TODO: Remove?
     * @param systemId ID of a system to be retrieved.
     * @return The desired system, if it is present in the local cache.
     */
    public SrSystem getSystem(int systemId) {
        if (!initialized) {
            throw new IllegalStateException("SystemTracker has not been initialized.");
        }
        return systemsById.get(systemId);
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
    public SrSystem getSystem(String name) {
        if (!initialized) {
            throw new IllegalStateException("SystemTracker has not been initialized.");
        }
        return systemsByName.get(name);
    }

}