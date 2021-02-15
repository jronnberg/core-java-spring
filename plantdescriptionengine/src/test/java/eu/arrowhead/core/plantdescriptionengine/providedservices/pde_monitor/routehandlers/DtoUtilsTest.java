package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortDto;
import eu.arrowhead.core.plantdescriptionengine.MonitorInfo;
import se.arkalix.description.ProviderDescription;
import se.arkalix.description.ServiceDescription;
import se.arkalix.descriptor.InterfaceDescriptor;
import se.arkalix.descriptor.SecurityDescriptor;
import se.arkalix.dto.json.value.JsonBoolean;
import se.arkalix.dto.json.value.JsonObject;
import se.arkalix.dto.json.value.JsonPair;

/**
 * Unit tests for the
 * {@link eu.arrowhead.core.plantdescriptionengine.utils.DtoUtils} class.
 */
public class DtoUtilsTest {

    @Test
    public void shouldExtendWithMonitorData() {

        final String systemName = "System A";

        final Map<String, String> metadata = Map.of("a", "b");
        final String portName = "Port-A";
        final String serviceDefinition = "Service-AC";
        final List<PortDto> ports = List.of(
            // Port B and C will *not* be complemented by monitor info:
            new PortBuilder()
                .metadata(Map.of("i", "j")) // Differs from service A
                .portName("Port-C")
                .serviceDefinition(serviceDefinition) // Same as service A
                .build(),
            new PortBuilder()
                .metadata(Map.of("x", "y")) // Differs from service A
                .portName("Port-B")
                .serviceDefinition("Service-B") // Differs from service A
                .build(),
            // Port A will be complemented by monitor info:
            new PortBuilder()
                .metadata(metadata)
                .portName(portName)
                .serviceDefinition(serviceDefinition)
                .build()
        );

        final PdeSystemDto system = new PdeSystemBuilder()
            .systemName(systemName)
            .systemId("system_a")
            .ports(ports)
            .build();

        final Instant now = Instant.now();
        final PlantDescriptionEntryDto entry = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(false)
            .include(new ArrayList<>())
            .systems(List.of(system))
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now)
            .build();

        final var provider = new ProviderDescription(systemName, new InetSocketAddress("0.0.0.0", 5000));
        ServiceDescription serviceDescription = new ServiceDescription.Builder()
            .name(serviceDefinition)
            .metadata(metadata)
            .uri("/abc")
            .security(SecurityDescriptor.NOT_SECURE)
            .provider(provider)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build();

        final String inventoryId = "system_a_inventory_id";
        JsonObject systemData = new JsonObject(List.of(
            new JsonPair("a", JsonBoolean.TRUE)
        ));

        final var monitorInfo = new MonitorInfo();
        monitorInfo.putInventoryId(serviceDescription, inventoryId);
        monitorInfo.putSystemData(serviceDescription, systemData);

        var extendedEntry = DtoUtils.extend(entry, monitorInfo);
        var extendedSystem = extendedEntry.systems().get(0);

        var extendedPortA = extendedSystem.ports().get(2);
        var extendedPortB = extendedSystem.ports().get(1);
        var extendedPortC = extendedSystem.ports().get(0);

        assertEquals(inventoryId, extendedPortA.inventoryId().get());
        assertEquals(systemData, extendedPortA.systemData().get());

        assertTrue(extendedPortB.inventoryId().isEmpty());
        assertTrue(extendedPortB.inventoryId().isEmpty());

        assertTrue(extendedPortC.inventoryId().isEmpty());
        assertTrue(extendedPortC.inventoryId().isEmpty());
    }


    @Test
    public void shouldExtendWithOutMonitorData() {

        final String systemName = "System A";

        final Map<String, String> metadata = Map.of("a", "b");
        final String portName = "Port-A";
        final String serviceDefinition = "Service-A";
        final List<PortDto> ports = List.of(
            new PortBuilder()
                .metadata(metadata)
                .portName(portName)
                .serviceDefinition(serviceDefinition)
                .build());
        final PdeSystemDto system = new PdeSystemBuilder()
            .systemName(systemName)
            .systemId("system_a")
            .ports(ports)
            .build();
        final Instant now = Instant.now();
        final PlantDescriptionEntryDto entry = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(false)
            .include(new ArrayList<>())
            .systems(List.of(system))
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now)
            .build();

        final var monitorInfo = new MonitorInfo();

        var extendedEntry = DtoUtils.extend(entry, monitorInfo);
        var extendedSystem = extendedEntry.systems().get(0);
        var extendedPort = extendedSystem.ports().get(0);
        assertTrue(extendedPort.inventoryId().isEmpty());
        assertTrue(extendedPort.systemData().isEmpty());
    }

    /**
     * In this test, the MonitorInfo instance contains data that can not be
     * matched to the system itself, since its metadata differs from that of
     * the system.
     */
    @Test
    public void shouldNotMatchInfoToSystem() {

        final String systemName = "System A";

        final Map<String, String> metadata = Map.of("a", "b");
        final String serviceDefinition = "Service-AC";

        final PdeSystemDto system = new PdeSystemBuilder()
            .systemName(systemName)
            .metadata(Map.of("foo", "bar"))
            .systemId("system_a")
            .build();

        final Instant now = Instant.now();
        final PlantDescriptionEntryDto entry = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(false)
            .include(new ArrayList<>())
            .systems(List.of(system))
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now)
            .build();

        final var provider = new ProviderDescription(systemName, new InetSocketAddress("0.0.0.0", 5000));
        ServiceDescription serviceDescription = new ServiceDescription.Builder()
            .name(serviceDefinition)
            .metadata(metadata)
            .uri("/abc")
            .security(SecurityDescriptor.NOT_SECURE)
            .provider(provider)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build();

        final String inventoryId = "system_a_inventory_id";
        JsonObject systemData = new JsonObject(List.of(
            new JsonPair("a", JsonBoolean.TRUE)
        ));

        final var monitorInfo = new MonitorInfo();
        monitorInfo.putInventoryId(serviceDescription, inventoryId);
        monitorInfo.putSystemData(serviceDescription, systemData);

        final var extendedEntry = DtoUtils.extend(entry, monitorInfo);
        var extendedSystem = extendedEntry.systems().get(0);
        assertTrue(extendedSystem.inventoryData().isEmpty());
        assertTrue(extendedSystem.systemData().isEmpty());

        // TODO: Assert that a warning has been logged.
    }

    // TODO: Add test to check that monitor info is only added to ports where the system is a provider.

}