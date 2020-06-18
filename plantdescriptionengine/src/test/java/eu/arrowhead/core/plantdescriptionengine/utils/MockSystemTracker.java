package eu.arrowhead.core.plantdescriptionengine.utils;

import java.net.InetSocketAddress;
import java.time.Instant;

import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystem;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystemBuilder;
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
    public SrSystem getSystem(String name) {
        return new SrSystemBuilder()
            .id(0)
            .systemName(name)
            .address("0.0.0.0")
            .port(5000)
            .authenticationInfo(null)
            .createdAt(Instant.now().toString())
            .updatedAt(Instant.now().toString())
            .build();
    }

}