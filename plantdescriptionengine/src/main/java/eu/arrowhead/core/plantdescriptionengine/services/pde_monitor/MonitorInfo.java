package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import eu.arrowhead.core.plantdescriptionengine.utils.DtoUtils;
import se.arkalix.description.ServiceDescription;
import se.arkalix.dto.json.value.JsonObject;

/**
 * Object used for keeping track of inventory data of monitorable systems.
 */
public class MonitorInfo {

    public static class Bundle {
        public final JsonObject systemData;
        public final Map<String, String> metadata;
        public final String inventoryId;
        public final String systemName;

        Bundle(
            String systemName, Map<String, String> metadata, JsonObject systemData, String inventoryId
        ) {
            this.systemData = systemData;
            this.inventoryId = inventoryId;
            this.systemName = systemName;
            this.metadata = metadata;
        }

        /**
         * Returns true if the given parameters matches this instances metadata.
         *
         * More specifically, returns true if {@code serviceMetadata} is
         * present, and the union of {@code systemMetadata} and
         * {@code serviceMetadata} is a subset of this instance's metadata.
         *
         * @param systemMetadata Metadata relating to a particular system
         *                       (read from a system in a Plant Description
         *                       Entry).
         * @param serviceMetadata Metadata relating to a particular service
         *                        (read from one of the ports of a system in a
         *                        Plant Description Entry).
         */
		public boolean matchesPort(
            Optional<Map<String, String>> systemMetadata, Optional<Map<String, String>> serviceMetadata
        ) {
            if (!serviceMetadata.isPresent() || serviceMetadata.get().size() == 0) {
                return false;
            }
            var mergedMetadata = DtoUtils.mergeMetadata(systemMetadata.orElse(new HashMap<>()), serviceMetadata.get());
            return isSubset(mergedMetadata, metadata);
		}
    }

    private final Map<String, Bundle> infoBundles = new ConcurrentHashMap<>();

    private String getKey(ServiceDescription service) {
        return service.provider().name() + service.uri();
    }

    /**
     * Returns
     * @param a A metadata object (mapping Strings to Strings).
     * @param b A metadata object (mapping Strings to Strings).
     *
     * @return True if a is a subset of b.
     */
    private static boolean isSubset(Map<String, String> a, Map<String, String> b) {
        for (String key : a.keySet()) {
            if (!b.containsKey(key) || !b.get(key).equals(a.get(key))) {
                return false;
            }
        }
        return true;
    }

    public void putInventoryId(ServiceDescription service, String inventoryId) {
        String systemName = service.provider().name();
        Map<String, String> metadata = service.metadata();
        String key = getKey(service);
        Bundle oldBundle = infoBundles.get(key);
        Bundle newBundle;
        if (oldBundle == null) {
            newBundle = new Bundle(systemName, metadata, null, inventoryId);
        } else {
            newBundle = new Bundle(systemName, metadata, oldBundle.systemData, inventoryId);
        }
        infoBundles.put(key, newBundle);
    }

    public void putSystemData(ServiceDescription service, JsonObject data) {
        String key = getKey(service);
        String systemName = service.provider().name();
        Map<String, String> metadata = service.metadata();
        Bundle oldBundle = infoBundles.get(key);
        Bundle newBundle;
        if (oldBundle == null) {
            newBundle = new Bundle(systemName, metadata, data, null);
        } else {
            newBundle = new Bundle(systemName, metadata, data, oldBundle.inventoryId);
        }
        infoBundles.put(key, newBundle);
    }

    public List<Bundle> getSystemInfo(String systemName, Map<String, String> metadata) {
        List<Bundle> result = new ArrayList<>();
        for (var bundle : infoBundles.values()) {
            if (systemName != null && systemName.equals(bundle.systemName)) {
                result.add(bundle);
            } else if (metadata != null && isSubset(metadata, bundle.metadata)) {
                result.add(bundle);
            }
        }
        return result;
    }

}