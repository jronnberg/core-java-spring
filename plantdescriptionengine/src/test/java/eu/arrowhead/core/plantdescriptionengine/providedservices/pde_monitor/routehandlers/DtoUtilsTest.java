package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import eu.arrowhead.core.plantdescriptionengine.MonitorInfoTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.MonitorPlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.MonitorPlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PortEntry;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.SystemEntry;
import org.junit.jupiter.api.Test;
import se.arkalix.ServiceInterface;
import se.arkalix.ServiceRecord;
import se.arkalix.SystemRecord;
import se.arkalix._internal.DefaultSystemRecord;
import se.arkalix.codec.json.JsonBoolean;
import se.arkalix.codec.json.JsonObject;
import se.arkalix.codec.json.JsonPair;
import se.arkalix.security.access.AccessPolicyType;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DtoUtilsTest {

    final private String systemName = "abc";
    private final Instant now = Instant.now();
    private final String someServiceDefinition = "servicexyz";
    private final String httpSecureJson = ServiceInterface.HTTP_SECURE_JSON.toString();

    private PdeSystemDto getSystemWithPorts(final List<PortDto> ports) {
        return new PdeSystemDto.Builder()
            .systemName(systemName)
            .systemId("system_a")
            .ports(ports)
            .build();
    }

    private ServiceRecord getServiceRecord(Map<String, String> systemMetadata, Map<String, String> serviceMetadata) {
        final SystemRecord provider = new DefaultSystemRecord(
            systemName, null, new InetSocketAddress("0.0.0.0", 5000), systemMetadata
        );
        return new ServiceRecord.Builder()
            .name(someServiceDefinition)
            .metadata(serviceMetadata)
            .uri("/abc")
            .provider(provider)
            .accessPolicyType(AccessPolicyType.NOT_SECURE)
            .interfaces(ServiceInterface.HTTP_SECURE_JSON)
            .build();
    }

    private PlantDescriptionEntryDto getEntryWithSystem(PdeSystemDto system) {
        return new PlantDescriptionEntryDto.Builder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(false)
            .systems(List.of(system))
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private boolean hasMonitorInfo(
        final PortEntry port,
        final String inventoryId,
        final JsonObject systemData
    ) {
        return inventoryId.equals(port.inventoryId().orElse(null))
            && systemData.equals(port.systemData().orElse(null));
    }

    private boolean hasMonitorInfo(
        final SystemEntry system,
        final String inventoryId,
        final JsonObject systemData
    ) {
        return inventoryId.equals(system.inventoryId().orElse(null))
            && systemData.equals(system.systemData().orElse(null));
    }

    private boolean hasNoMonitorInfo(final PortEntry port) {
        return port.inventoryId().isEmpty() && port.systemData().isEmpty();
    }

    @Test
    public void shouldPlaceMonitorDataOnPortLevel() throws PdStoreException {
        final Map<String, String> serviceMetadata = Map.of("a", "b");
        final List<PortDto> ports = List.of(
            // Port B and C will *not* be complemented by monitor info:
            new PortDto.Builder()
                .metadata(Map.of("i", "j")) // Differs
                .portName("Port-A")
                .serviceInterface(httpSecureJson)
                .serviceDefinition(someServiceDefinition)
                .build(),
            new PortDto.Builder()
                .metadata(serviceMetadata)
                .portName("Port-B")
                .serviceInterface(httpSecureJson)
                .serviceDefinition("Service-B") // Differs
                .build(),
            // Port A has matching metadata *and* service definition,
            // so it should be matched.
            new PortDto.Builder()
                .metadata(serviceMetadata)
                .portName("Port-C")
                .serviceInterface(httpSecureJson)
                .serviceDefinition(someServiceDefinition)
                .build()
        );

        final PdeSystemDto system = getSystemWithPorts(ports);
        final PlantDescriptionEntryDto entry = getEntryWithSystem(system);
        final ServiceRecord ServiceRecord = getServiceRecord(null, serviceMetadata);
        final String inventoryId = "abc_inventory_id";
        final JsonObject systemData = new JsonObject(new JsonPair("a", JsonBoolean.TRUE));
        final MonitorInfoTracker monitorInfoTracker = new MonitorInfoTracker();
        final PlantDescriptionTracker pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());

        monitorInfoTracker.putInventoryId(ServiceRecord, inventoryId);
        monitorInfoTracker.putSystemData(ServiceRecord, systemData);
        pdTracker.put(entry);

        final MonitorPlantDescriptionEntry extendedEntry = DtoUtils.extend(entry, monitorInfoTracker, pdTracker);
        final SystemEntry extendedSystem = extendedEntry.systems().get(0);

        final PortEntry extendedPortA = extendedSystem.ports().get(0);
        final PortEntry extendedPortB = extendedSystem.ports().get(1);
        final PortEntry extendedPortC = extendedSystem.ports().get(2);

        assertTrue(hasNoMonitorInfo(extendedPortA));
        assertTrue(hasNoMonitorInfo(extendedPortB));
        assertTrue(hasMonitorInfo(extendedPortC, inventoryId, systemData));
    }

    @Test
    public void shouldPlaceMonitorDataOnSystemLevel() throws PdStoreException {

        final Map<String, String> systemMetadata = Map.of("x", "0", "y", "1", "z", "2");
        final Map<String, String> serviceMetadata = Map.of("foo", "bar");
        final ServiceRecord ServiceRecord = getServiceRecord(systemMetadata, serviceMetadata);
        final List<PortDto> ports = List.of(
            // This port has metadata that differs from that of the service
            // record's system metadata.
            new PortDto.Builder()
                .metadata(Map.of("i", "j"))
                .portName("Port-A")
                .serviceInterface(httpSecureJson)
                .serviceDefinition(someServiceDefinition)
                .build(),
            // This port has the same metadata as the service record's system
            // metadata, but it should still not be extended with monitor info:
            // the *service* metadata of the service record does not match.
            new PortDto.Builder()
                .metadata(systemMetadata)
                .portName("Port-B")
                .serviceInterface(httpSecureJson)
                .serviceDefinition(someServiceDefinition)
                .build()
        );

        final PdeSystemDto system = getSystemWithPorts(ports);
        final PlantDescriptionEntryDto entry = getEntryWithSystem(system);

        final String inventoryId = "abc_inventory_id";
        final JsonObject systemData = new JsonObject(new JsonPair("a", JsonBoolean.TRUE));
        final MonitorInfoTracker monitorInfoTracker = new MonitorInfoTracker();
        final PlantDescriptionTracker pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());

        monitorInfoTracker.putInventoryId(ServiceRecord, inventoryId);
        monitorInfoTracker.putSystemData(ServiceRecord, systemData);
        pdTracker.put(entry);

        final MonitorPlantDescriptionEntry extendedEntry = DtoUtils.extend(entry, monitorInfoTracker, pdTracker);
        final SystemEntry extendedSystem = extendedEntry.systems().get(0);

        final PortEntry extendedPortA = extendedSystem.ports().get(0);
        final PortEntry extendedPortB = extendedSystem.ports().get(1);

        assertTrue(hasNoMonitorInfo(extendedPortA));
        assertTrue(hasNoMonitorInfo(extendedPortB));
        assertTrue(hasMonitorInfo(extendedSystem, inventoryId, systemData));
    }

    @Test
    public void shouldExtendWithoutMonitorData() throws PdStoreException {
        final PortDto port = new PortDto.Builder()
            .portName("Port-A")
            .serviceInterface(httpSecureJson)
            .serviceDefinition(someServiceDefinition)
            .build();
        final PdeSystemDto system = getSystemWithPorts(List.of(port));
        final PlantDescriptionEntryDto entry = getEntryWithSystem(system);
        final MonitorInfoTracker monitorInfoTracker = new MonitorInfoTracker();
        final PlantDescriptionTracker pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());

        pdTracker.put(entry);

        final MonitorPlantDescriptionEntryDto extendedEntry = DtoUtils.extend(entry, monitorInfoTracker, pdTracker);
        final SystemEntry extendedSystem = extendedEntry.systems().get(0);
        final PortEntry extendedPort = extendedSystem.ports().get(0);

        assertTrue(extendedPort.inventoryId().isEmpty());
        assertTrue(extendedPort.systemData().isEmpty());
        assertEquals(httpSecureJson, extendedPort.serviceInterface().orElse(null));
    }

    /**
     * Test that consumer ports are not supplemented with monitor info. In fact,
     * Plant Descriptions with consumer ports containing metadata are not
     * allowed. This is enforced by the {@link eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.PlantDescriptionValidator}.
     */
    @Test
    public void shouldNotAddMonitorDataToConsumerPort() throws PdStoreException {
        final Map<String, String> serviceMetadata = Map.of("a", "b");
        final PortDto port = new PortDto.Builder()
            .metadata(serviceMetadata)
            .portName("Port-A")
            .consumer(true)
            .serviceInterface(httpSecureJson)
            .serviceDefinition(someServiceDefinition)
            .build();

        final PdeSystemDto system = getSystemWithPorts(List.of(port));
        final PlantDescriptionEntryDto entry = getEntryWithSystem(system);
        final String inventoryId = "abc_inventory_id";
        final JsonObject systemData = new JsonObject(new JsonPair("a", JsonBoolean.TRUE));
        final MonitorInfoTracker monitorInfoTracker = new MonitorInfoTracker();
        final PlantDescriptionTracker pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final ServiceRecord ServiceRecord = getServiceRecord(null, serviceMetadata);

        monitorInfoTracker.putInventoryId(ServiceRecord, inventoryId);
        monitorInfoTracker.putSystemData(ServiceRecord, systemData);
        pdTracker.put(entry);

        final MonitorPlantDescriptionEntryDto extendedEntry = DtoUtils.extend(entry, monitorInfoTracker, pdTracker);
        final SystemEntry extendedSystem = extendedEntry.systems().get(0);
        final PortEntry extendedPortA = extendedSystem.ports().get(0);

        assertTrue(hasNoMonitorInfo(extendedPortA));
    }

}