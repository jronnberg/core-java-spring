package eu.arrowhead.core.plantdescriptionengine.utils;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystem;
import se.arkalix.net.http.client.HttpClient;

/**
 * Subclass of SystemTracker used for testing purposes.
 */
public class MockSystemTracker extends SystemTracker {

    // System ID -> system
    Map<String, SrSystem> systems = new HashMap<>();

    public MockSystemTracker(HttpClient httpClient, InetSocketAddress serviceRegistryAddress) {
        super(httpClient, serviceRegistryAddress);
    }

    /**
     * @param systemName ID of a system to be retrieved.
     * @return A mock system with the specified name.
     */
    public SrSystem getSystem(String systemId) {
        return (systems.get(systemId));
    }

    /**
     * Adds a new system to the system tracker.
     *
     * @param systemId ID of the system to add.
     * @param system The system to add.
     */
    public void addSystem(String systemId, SrSystem system) {
        systems.put(systemId, system);
	}

}