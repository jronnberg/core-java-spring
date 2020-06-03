package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor;

import org.junit.Test;

import se.arkalix.description.ProviderDescription;
import se.arkalix.description.ServiceDescription;
import se.arkalix.descriptor.InterfaceDescriptor;
import se.arkalix.descriptor.SecurityDescriptor;
import se.arkalix.dto.json.value.JsonObject;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MonitorInfoTest {

    @Test
    public void shouldMatch() {

        String systemName = "System A";
        Map<String, String> metadata = new HashMap<>();
        JsonObject systemData = null;
        String inventoryId = null;

        metadata.put("a", "1");
        metadata.put("b", "2");

        var info = new MonitorInfo.Bundle(systemName, metadata, systemData, inventoryId);

        Map<String, String> systemMetadata = new HashMap<>();
        Map<String, String> serviceMetadata = new HashMap<>();

        systemMetadata.put("a", "1");
        serviceMetadata.put("b", "2");

        assertTrue(info.matchesPort(Optional.of(systemMetadata), Optional.of(serviceMetadata)));
    }

    @Test
    public void shouldNotMatch() {

        String systemName = "System A";
        Map<String, String> metadata = new HashMap<>();
        JsonObject systemData = null;
        String inventoryId = null;

        metadata.put("a", "1");
        metadata.put("b", "2");

        var info = new MonitorInfo.Bundle(systemName, metadata, systemData, inventoryId);

        Map<String, String> systemMetadata = new HashMap<>();
        Map<String, String> serviceMetadata = new HashMap<>();

        systemMetadata.put("a", "x");
        serviceMetadata.put("b", "2");

        assertFalse(info.matchesPort(Optional.of(systemMetadata), Optional.of(serviceMetadata)));
    }

    @Test
    public void supersetShouldMatch() {

        String systemName = "System A";
        Map<String, String> metadata = new HashMap<>();
        JsonObject systemData = null;
        String inventoryId = null;

        metadata.put("a", "1");
        metadata.put("b", "2");
        metadata.put("c", "3");

        var info = new MonitorInfo.Bundle(systemName, metadata, systemData, inventoryId);

        Map<String, String> systemMetadata = new HashMap<>();
        Map<String, String> serviceMetadata = new HashMap<>();

        systemMetadata.put("a", "1");
        serviceMetadata.put("b", "2");

        assertTrue(info.matchesPort(Optional.of(systemMetadata), Optional.of(serviceMetadata)));
    }

    @Test
    public void subsetsShouldNotMatch() {

        String systemName = "System A";
        Map<String, String> metadata = new HashMap<>();
        JsonObject systemData = null;
        String inventoryId = null;

        metadata.put("a", "1");
        metadata.put("b", "2");

        var info = new MonitorInfo.Bundle(systemName, metadata, systemData, inventoryId);

        Map<String, String> systemMetadata = new HashMap<>();
        Map<String, String> serviceMetadata = new HashMap<>();

        systemMetadata.put("a", "1");
        serviceMetadata.put("b", "2");
        serviceMetadata.put("c", "3");

        assertFalse(info.matchesPort(Optional.of(systemMetadata), Optional.of(serviceMetadata)));
    }

    @Test
    public void shouldRequireServiceMetadata() {

        String systemName = "System A";
        Map<String, String> metadata = new HashMap<>();
        JsonObject systemData = null;
        String inventoryId = null;

        metadata.put("a", "1");

        var info = new MonitorInfo.Bundle(systemName, metadata, systemData, inventoryId);

        Map<String, String> systemMetadata = new HashMap<>();
        Map<String, String> serviceMetadata = new HashMap<>();

        systemMetadata.put("a", "1");

        assertFalse(info.matchesPort(Optional.of(systemMetadata), Optional.of(serviceMetadata)));
        assertFalse(info.matchesPort(Optional.of(systemMetadata), Optional.empty()));
    }

    @Test
    public void serviceShouldOverrideSystem() {

        String systemName = "System A";
        Map<String, String> metadata = new HashMap<>();
        JsonObject systemData = null;
        String inventoryId = null;

        metadata.put("a", "1");

        var info = new MonitorInfo.Bundle(systemName, metadata, systemData, inventoryId);

        Map<String, String> systemMetadata = new HashMap<>();
        Map<String, String> serviceMetadata = new HashMap<>();

        systemMetadata.put("a", "2");
        serviceMetadata.put("a", "1");

        assertTrue(info.matchesPort(Optional.of(systemMetadata), Optional.of(serviceMetadata)));

        systemMetadata.put("a", "1");
        serviceMetadata.put("a", "2");

        assertFalse(info.matchesPort(Optional.of(systemMetadata), Optional.of(serviceMetadata)));

    }

    @Test
    public void shouldStoreInventoryIds() {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("x", "y");

        var provider = new ProviderDescription("Provider", new InetSocketAddress("0.0.0.0", 5000));
        ServiceDescription  serviceDescription = new ServiceDescription.Builder()
            .name("service-a")
            .provider(provider)
            .uri("/test")
            .security(SecurityDescriptor.NOT_SECURE)
            .interfaces(List.of(InterfaceDescriptor.HTTP_SECURE_JSON))
            .metadata(metadata)
            .build();

        String inventoryId = "id-1234";
        var monitorInfo = new MonitorInfo();
        monitorInfo.putInventoryId(serviceDescription, inventoryId);

        Map<String, String> lookupMetadata = new HashMap<>();
        lookupMetadata.put("x", "y");

        var systemInfoList = monitorInfo.getSystemInfo(null, lookupMetadata);
        assertEquals(1, systemInfoList.size());

        var systemInfo = systemInfoList.get(0);
        assertEquals(inventoryId, systemInfo.inventoryId);
    }
}

