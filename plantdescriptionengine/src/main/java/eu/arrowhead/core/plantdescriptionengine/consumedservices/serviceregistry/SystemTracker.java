package eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry;

import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystem;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystemListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object used to keep track of registered Arrowhead systems.
 */
public class SystemTracker {
    private static final Logger logger = LoggerFactory.getLogger(SystemTracker.class);
    // List of instances that need to be informed when systems are added or
    // removed from the service registry.
    protected final List<SystemUpdateListener> listeners = new ArrayList<>();
    // Map from system name to system:
    protected final Map<String, SrSystem> systems = new ConcurrentHashMap<>();
    private final HttpClient httpClient;
    private final InetSocketAddress serviceRegistryAddress;
    private final int pollInterval = 5000;
    private boolean initialized;

    /**
     * Class constructor
     *
     * @param httpClient             Object for communicating with the Service
     *                               Registry.
     * @param serviceRegistryAddress Address of the Service Registry.
     */
    public SystemTracker(final HttpClient httpClient, final InetSocketAddress serviceRegistryAddress) {

        Objects.requireNonNull(serviceRegistryAddress, "Expected service registry address");
        Objects.requireNonNull(httpClient, "Expected HTTP client");

        this.httpClient = httpClient;
        this.serviceRegistryAddress = serviceRegistryAddress;
    }

    /**
     * @return A Future which will complete with a list of registered systems.
     * <p>
     * The retrieved systems are stored locally, and can be accessed using
     * {@link #getSystems()} or {@link #getSystemByName(String) }.
     */
    private Future<Void> fetchSystems() {
        return httpClient
            .send(serviceRegistryAddress,
                new HttpClientRequest().method(HttpMethod.GET).uri("/serviceregistry/mgmt/systems").header("accept",
                    "application/json"))
            .flatMap(response -> response.bodyAsClassIfSuccess(DtoEncoding.JSON, SrSystemListDto.class))
            .flatMap(systemList -> {
                List<SrSystem> newSystems = systemList.data();
                List<SrSystem> oldSystems = new ArrayList<>(systems.values());

                // Replace the stored list of registered systems.
                systems.clear();
                for (var system : newSystems) {
                    systems.put(system.systemName(), system);
                }
                initialized = true;
                notifyListeners(oldSystems, newSystems);
                return Future.done();
            });
    }

    /**
     * Informs all registered listeners of which systems have been added or removed
     * from the service registry since the last refresh.
     *
     * @param oldSystems Systems that were previously present in the Service
     *                   Registry.
     * @param newSystems Systems that are present in the Service Registry.
     */
    private void notifyListeners(List<SrSystem> oldSystems, List<SrSystem> newSystems) {
        // Report removed systems
        for (var oldSystem : oldSystems) {
            boolean stillPresent = newSystems.stream()
                .anyMatch(newSystem -> newSystem.systemName().equals(oldSystem.systemName()));
            if (!stillPresent) {
                for (var listener : listeners) {
                    logger.info("System '" + oldSystem.systemName() + "' has been removed from the Service Registry.");
                    listener.onSystemRemoved(oldSystem);
                }
            }
        }

        // Report added systems
        for (var newSystem : newSystems) {
            boolean wasPresent = oldSystems.stream()
                .anyMatch(oldSystem -> newSystem.systemName().equals(oldSystem.systemName()));
            if (!wasPresent) {
                logger.info("System '" + newSystem.systemName() + "' detected in Service Registry.");
                for (var listener : listeners) {
                    listener.onSystemAdded(newSystem);
                }
            }
        }
    }

    /**
     * Registers another object to be notified whenever a system is added or
     * removed.
     *
     * @param listener Object to notify.
     */
    public void addListener(SystemUpdateListener listener) {
        listeners.add(listener);
    }

    /**
     * Retrieves the specified system. Note that the returned data will be stale if
     * the system in question has changed state since the last call to
     * {@link #fetchSystems()}.
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

    public List<SrSystem> getSystems() {
        return new ArrayList<>(systems.values());
    }

    /**
     * Starts polling the Service Registry for registered systems.
     *
     * @return A Future that completes on the first reply from the Service Registry.
     */
    public Future<Void> start() {
        return fetchSystems().flatMap(result -> {
            // Periodically poll the Service Registry for systems.
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    fetchSystems()
                    .onFailure(error -> {
                        logger.error("Failed to retrieve registered systems", error);
                    });
                }
            }, pollInterval, pollInterval);

            return Future.done();
        });
    }

}