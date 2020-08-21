package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
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
    private int pollInterval = 5000;

    // List of instances that need to be informed when systems are added or
    // removed from the service registry.
    protected List<SystemUpdateListener> listeners = new ArrayList<>();

    // Map from system name to system:
    protected Map<String, SrSystem> systems = new ConcurrentHashMap<>();

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
    private Future<Void> pollForSystems() {
        return httpClient.send(serviceRegistryAddress, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/serviceregistry/mgmt/systems")
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, SrSystemListDto.class))
            .flatMap(systemList -> {
                reportChanges(systemList); // TODO: Report changes after the system list has been updated instead?
                systems.clear();
                for (var system : systemList.data()) {
                    systems.put(system.systemName(), system);
                }
                initialized = true;
                return Future.done();
            });
    }

    /**
     * Informs all registered listeners of which systems have been added or
     * removed from the service registry since the last refresh.
     *
     * @param newSystems
     */
    private void reportChanges(SrSystemListDto newSystems) {
        // Report removed systems
        for (var oldSystem: systems.values()) {
            boolean stillPresent = newSystems.data()
                .stream()
                .anyMatch(newSystem -> newSystem.systemName().equals(oldSystem.systemName()));
            if (!stillPresent) {
                for (var listener: listeners) {
                    listener.onSystemRemoved(oldSystem);
                }
            }
        }

        // Report added systems
        for (var newSystem : newSystems.data()) {
            boolean wasPresent = systems.values()
                .stream()
                .anyMatch(oldSystem -> newSystem.systemName().equals(oldSystem.systemName()));
            if (!wasPresent) {
                for (var listener: listeners) {
                    listener.onSystemAdded(newSystem);
                }
            }
        }
    }

    /**
     * Retrieves the specified system. Note that the returned data will be stale if
     * the system in question has changed state since the last call to
     * {@link #pollForSystems()}.
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
     * Registers another object to be notified whenever a system is added or
     * removed.
     *
     * @param listener
     */
    public void addListener(SystemUpdateListener listener) {
        listeners.add(listener);
    }

    public List<SrSystem> getSystems() {
        return new ArrayList<SrSystem>(systems.values());
    }

    /**
     * Starts polling the Service Registry for registered systems.
     * @return A Future that completes on the first reply from the Service
     *         Registry.
     */
    public Future<Void> startPollingForSystems() {
        final var timer = new Timer();

        return pollForSystems().flatMap(result -> {

            // Periodically poll the Service Registry for systems.
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    pollForSystems();
                }
            }, 0, pollInterval);

            return Future.done();
        });
    }

}