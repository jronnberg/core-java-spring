package eu.arrowhead.core.plantdescriptionengine;

import eu.arrowhead.core.plantdescriptionengine.utils.Metadata;
import se.arkalix.ServiceRecord;
import se.arkalix.SystemRecord;
import se.arkalix.codec.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object used for keeping track of inventory data of monitorable systems.
 */
public class MonitorInfoTracker {

    private final Map<String, MonitorInfo> trackedInfo = new ConcurrentHashMap<>();

    /**
     * @param service An Arrowhead Framework service.
     * @return A unique identifier for the given service.
     */
    private String toKey(final ServiceRecord service) {

        Map<String, String> metadata = service.provider().metadata();
        String result = "name=" + service.provider().name() + ",serviceUri=" + service.uri();

        if (metadata != null) {
            result += ",metadata={" + Metadata.toString(metadata);
        }

        return result + "}";
    }

    /**
     * Store the inventory ID of a given service. Any inventory ID previously
     * stored for this system will be overwritten.
     *
     * @param service     An Arrowhead Framework service.
     * @param inventoryId An inventory ID.
     */
    public void putInventoryId(final ServiceRecord service, final String inventoryId) {

        Objects.requireNonNull(service, "Expected service");
        Objects.requireNonNull(service, "Expected inventory ID");

        final String systemName = service.provider().name();
        final Map<String, String> serviceMetadata = service.metadata();
        final Map<String, String> systemMetadata = service.provider().metadata();

        final String key = toKey(service);
        final MonitorInfo oldInfo = trackedInfo.get(key);
        final JsonObject systemData = (oldInfo == null) ? null : oldInfo.systemData;

        final MonitorInfo newInfo = new MonitorInfo(
            systemName,
            service.name(),
            systemMetadata,
            serviceMetadata,
            systemData,
            inventoryId
        );

        trackedInfo.put(key, newInfo);
    }

    /**
     * Store system data for the given service. Any system data previously
     * stored for this system will be overwritten.
     *
     * @param service An Arrowhead Framework service.
     * @param data    System data to be stored.
     */
    public void putSystemData(final ServiceRecord service, final JsonObject data) {

        Objects.requireNonNull(service, "Expected service");
        Objects.requireNonNull(data, "Expected system data");

        final String key = toKey(service);
        final SystemRecord provider = service.provider();
        final String systemName = provider.name();

        final Map<String, String> systemMetadata = provider.metadata();
        final Map<String, String> serviceMetadata = service.metadata();
        final String serviceDefinition = service.name();
        final MonitorInfo oldInfo = trackedInfo.get(key);
        final String inventoryId = (oldInfo == null) ? null : oldInfo.inventoryId;

        final MonitorInfo monitorInfo = new MonitorInfo(
            systemName,
            serviceDefinition,
            systemMetadata,
            serviceMetadata,
            data,
            inventoryId
        );

        trackedInfo.put(key, monitorInfo);
    }

    /**
     * @param systemName Name of an Arrowhead Framework system. May be null if
     *                   {@code metadata} is present.
     * @param metadata   Metadata describing an Arrowhead Framework system. May
     *                   be null if {@code systemName} is present.
     * @return A list containing all stored monitor data for the system that
     * corresponds to the given arguments.
     */
    public List<MonitorInfo> getSystemInfo(final String systemName, final Map<String, String> metadata) {

        if (systemName == null && metadata == null) {
            throw new IllegalArgumentException("Either system name or metadata must be present.");
        }

        final List<MonitorInfo> result = new ArrayList<>();
        for (final MonitorInfo info : trackedInfo.values()) {
            final boolean namesMatch = systemName == null || systemName.equals(info.systemName);
            // TODO: Make the method below take the name as an argument as well.
            final boolean metadataMatch = info.matchesSystemMetadata(metadata);
            if (namesMatch && metadataMatch) {
                result.add(info);
            }
        }
        return result;
    }

}