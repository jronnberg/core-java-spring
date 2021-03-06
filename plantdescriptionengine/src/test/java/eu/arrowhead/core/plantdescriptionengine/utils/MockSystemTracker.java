package eu.arrowhead.core.plantdescriptionengine.utils;

import java.net.InetSocketAddress;

import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystem;
import se.arkalix.net.http.client.HttpClient;

/**
 * Subclass of SystemTracker used for testing purposes.
 */
public class MockSystemTracker extends SystemTracker {

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
        for (var listener: listeners) {
            listener.onSystemAdded(system);
        }
    }

}