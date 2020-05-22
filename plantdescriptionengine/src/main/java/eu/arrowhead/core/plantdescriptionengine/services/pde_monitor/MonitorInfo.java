package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public void putInventoryId(String systemName, String id) {
        inventoryIds.put(systemName, id);
    }

    public void removeInventoryId(String systemName) {
        inventoryIds.remove(systemName);
    }

    public Map<String, String> getSystemData(String systemName) {
        return systemData.get(systemName);
    }

    public void putSystemData(String systemName, Map<String, String> data) {
        systemData.put(systemName, data);
    }

    public void removeSystemData(String systemName) {
        systemData.remove(systemName);
    }
}