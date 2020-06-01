package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.arkalix.description.ServiceDescription;

/**
 * Object used for keeping track of inventory data of monitorable systems.
 */
public class MonitorInfo {

    // Map relating system names to inventory IDs:
    private final Map<String, String> inventoryIds = new ConcurrentHashMap<>();

    // Map relating system names to data:
    private final Map<String, Map<String, String>> systemData = new ConcurrentHashMap<>();

    // TODO: Use IDs as keys instead of names

    public String getInventoryId(String systemName) {
        return inventoryIds.get(systemName);
    }

    public void putInventoryId(ServiceDescription service, String inventoryId) {
        inventoryIds.put(service.provider().name(), inventoryId);
    }

    public void removeInventoryId(String systemName) {
        inventoryIds.remove(systemName);
    }

    public Map<String, String> getSystemData(String systemName) {
        return systemData.get(systemName);
    }

    public void putSystemData(ServiceDescription service, Map<String, String> data) {
        systemData.put(service.provider().name(), data);
    }

    public void removeSystemData(String systemName) {
        systemData.remove(systemName);
    }
}