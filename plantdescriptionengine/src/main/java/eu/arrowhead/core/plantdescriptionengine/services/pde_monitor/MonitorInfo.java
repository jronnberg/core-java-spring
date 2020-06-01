package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.arkalix.description.ServiceDescription;

/**
 * Object used for keeping track of inventory data of monitorable systems.
 */
public class MonitorInfo {

    // Map relating the tuple (systemName, serviceUri) to inventory IDs:
    private final Map<String, String> inventoryIds = new ConcurrentHashMap<>();

    // Map relating the tuple (systemName, serviceUri) to system data:
    private final Map<String, Map<String, String>> systemData = new ConcurrentHashMap<>();

    public void putInventoryId(ServiceDescription service, String inventoryId) {
        inventoryIds.put(service.provider().name() + service.uri(), inventoryId);
    }

    public void putSystemData(ServiceDescription service, Map<String, String> data) {
        systemData.put(service.provider().name() + service.uri(), data);
    }

}