package eu.arrowhead.core.plantdescriptionengine.utils;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystem;
import se.arkalix.net.http.client.HttpClient;

/**
 * Subclass of SystemTracker used for testing purposes.
 */
public class MockSystemTracker extends SystemTracker {

    // Map from system name to system:
    Map<String, SrSystem> systems = new HashMap<>();

    public MockSystemTracker(HttpClient httpClient, InetSocketAddress serviceRegistryAddress) {
        super(httpClient, serviceRegistryAddress);
    }

    /**
     * @param systemName Name of a system to be retrieved.
     * @return A mock system with the specified name.
     */
    @Override
    public SrSystem getSystemByName(String systemName) {
        return systems.get(systemName);
    }

    /**
     * Adds a new system to the system tracker.
     *
     * @param systemId ID of the system to add.
     * @param system The system to add.
     */
    public void addSystem(String systemName, SrSystem system) {
        systems.put(systemName, system);
	}

}