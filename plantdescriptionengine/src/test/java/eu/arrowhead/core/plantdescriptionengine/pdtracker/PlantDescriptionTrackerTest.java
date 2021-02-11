package eu.arrowhead.core.plantdescriptionengine.pdtracker;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.ConnectionBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.ConnectionDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.SystemPortBuilder;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;

/**
 * Unit test for the
 * {@link eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.PlantDescriptionEntry}
 * class.
 */
public class PlantDescriptionTrackerTest {

    final Instant now = Instant.now();

    PdStore store;
    PlantDescriptionTracker pdTracker;

    final class Listener implements PlantDescriptionUpdateListener {

        int numAdded = 0;
        int numUpdated = 0;
        int numRemoved = 0;

        // Store the IDs of entries:
        PlantDescriptionEntry lastAdded = null;
        PlantDescriptionEntry lastUpdated = null;
        PlantDescriptionEntry lastRemoved = null;

        @Override
        public void onPlantDescriptionAdded(PlantDescriptionEntry entry) {
            lastAdded = entry;
            numAdded++;
        }

        @Override
        public void onPlantDescriptionUpdated(PlantDescriptionEntry entry) {
            lastUpdated = entry;
            numUpdated++;
        }

        @Override
        public void onPlantDescriptionRemoved(PlantDescriptionEntry entry) {
            lastRemoved = entry;
            numRemoved++;
        }
    }

    @BeforeEach
    public void initEach() throws PdStoreException {
        store = new InMemoryPdStore();
        pdTracker = new PlantDescriptionTracker(store);
    }

    @Test
    public void shouldReadEntriesFromBackingStore() throws PdStoreException {
        final List<Integer> entryIds = List.of(1, 2, 3);

        for (int id : entryIds) {
            store.write(TestUtils.createEntry(id));
        }

        final var newPdTracker = new PlantDescriptionTracker(store);
        var storedEntries = newPdTracker.getEntries();

        assertEquals(3, storedEntries.size());
    }

    @Test
    public void shouldReturnEntryById() throws PdStoreException {
        int entryId = 16;
        final PlantDescriptionEntryDto entry = TestUtils.createEntry(entryId);
        pdTracker.put(entry);

        final var storedEntry = pdTracker.get(entryId);

        assertNotNull(storedEntry);
        assertEquals(entryId, storedEntry.id(), 0);
    }

    @Test
    public void shouldReturnAllEntries() throws PdStoreException {

        final List<Integer> entryIds = List.of(16, 39, 244);

        for (int id : entryIds) {
            pdTracker.put(TestUtils.createEntry(id));
        }

        final var storedEntries = pdTracker.getEntries();

        assertEquals(entryIds.size(), storedEntries.size());

        for (final var entry : storedEntries) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void shouldReturnListDto() throws PdStoreException {

        final List<Integer> entryIds = List.of(16, 39, 244);
        for (int id : entryIds) {
            pdTracker.put(TestUtils.createEntry(id));
        }

        final var storedEntries = pdTracker.getListDto();

        assertEquals(entryIds.size(), storedEntries.data().size());

        for (final var entry : storedEntries.data()) {
            assertTrue(entryIds.contains(entry.id()));
        }
    }

    @Test
    public void shouldRemoveEntries() throws PdStoreException {
        int entryId = 24;
        final PlantDescriptionEntryDto entry = TestUtils.createEntry(entryId);
        pdTracker.put(entry);
        pdTracker.remove(entryId);
        final var storedEntry = pdTracker.get(entryId);

        assertNull(storedEntry);
    }

    @Test
    public void shouldTrackActiveEntry() throws PdStoreException {
        final var builder = new PlantDescriptionEntryBuilder()
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now);
        final var activeEntry = builder
            .id(1)
            .plantDescription("Plant Description A")
            .active(true)
            .build();
        final var inactiveEntry = builder
            .id(2)
            .plantDescription("Plant Description B")
            .active(false)
            .build();

        pdTracker.put(activeEntry);
        pdTracker.put(inactiveEntry);

        assertNotNull(pdTracker.activeEntry());
        assertEquals(activeEntry.id(), pdTracker.activeEntry().id(), 0);

        pdTracker.remove(activeEntry.id());

        assertNull(pdTracker.activeEntry());
    }

    @Test
    public void shouldDeactivateEntry() throws PdStoreException {
        final int idA = 1;
        final int idB = 2;
        final var builder = new PlantDescriptionEntryBuilder()
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .active(true)
            .createdAt(now)
            .updatedAt(now);
        final var entryA = builder
            .id(idA)
            .plantDescription("Plant Description A")
            .build();
        final var entryB = builder
            .id(idB)
            .plantDescription("Plant Description B")
            .build();

        pdTracker.put(entryA);

        assertTrue(pdTracker.get(idA).active());
        pdTracker.put(entryB);
        assertFalse(pdTracker.get(idA).active());
    }

    @Test
    public void shouldGenerateUniqueIds() throws PdStoreException {

        final List<PlantDescriptionEntryDto> entries = List.of(
            TestUtils.createEntry(pdTracker.getUniqueId()),
            TestUtils.createEntry(pdTracker.getUniqueId()),
            TestUtils.createEntry(pdTracker.getUniqueId())
        );

        for (final var entry : entries) {
            pdTracker.put(entry);
        }

        int uid = pdTracker.getUniqueId();

        for (final var entry : entries) {
            assertNotEquals(uid, entry.id());
        }
    }

    @Test
    public void shouldNotifyOnAdd() throws PdStoreException {

        final var listener = new Listener();

        final int idA = 16;
        final int idB = 32;

        pdTracker.addListener(listener);
        pdTracker.put(TestUtils.createEntry(idA));
        pdTracker.put(TestUtils.createEntry(idB));

        assertEquals(idB, listener.lastAdded.id());
        assertEquals(2, listener.numAdded);
    }

    @Test
    public void shouldNotifyOnDelete() throws PdStoreException {

        final var listener = new Listener();

        final int idA = 5;
        final int idB = 12;

        pdTracker.addListener(listener);
        pdTracker.put(TestUtils.createEntry(idA));
        pdTracker.put(TestUtils.createEntry(idB));
        pdTracker.remove(idA);

        assertEquals(idA, listener.lastRemoved.id());
        assertEquals(1, listener.numRemoved);
    }


    @Test
    public void shouldNotifyOnUpdate() throws PdStoreException {

        final var listener = new Listener();

        final int idA = 93;

        pdTracker.addListener(listener);
        pdTracker.put(TestUtils.createEntry(idA));
        // "Update" the entry by putting an identical copy in the tracker.
        pdTracker.put(TestUtils.createEntry(idA));

        assertEquals(idA, listener.lastUpdated.id());
        assertEquals(1, listener.numUpdated);
    }

    @Test
    public void shouldNotifyWhenActiveEntryChanges() throws PdStoreException {

        final var listener = new Listener();

        final int idA = 2;
        final int idB = 8;

        pdTracker.addListener(listener);

        // Add an active entry.
        pdTracker.put(TestUtils.createEntry(idA, true));
        assertEquals(idA, listener.lastAdded.id());
        assertTrue(listener.lastAdded.active());

        // Add another active entry.
        pdTracker.put(TestUtils.createEntry(idB, true));

        // Listeners should have been notified that the old one was deactivated.
        assertEquals(idA, listener.lastUpdated.id());
        assertFalse(listener.lastUpdated.active());

        assertEquals(1, listener.numUpdated);
    }

    @Test
    public void shouldReturnTheCorrectSystem() throws PdStoreException {
        final String idA = "Sys-A";
        final String idB = "Sys-B";
        final String idC = "Sys-C";

        final PdeSystemDto systemA = new PdeSystemBuilder()
            .systemId(idA)
            .build();

        final PdeSystemDto systemB = new PdeSystemBuilder()
            .systemId(idB)
            .build();

        final PdeSystemDto systemC = new PdeSystemBuilder()
            .systemId(idC)
            .build();

        final var entry = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("ABC")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .systems(List.of(systemA, systemB, systemC))
            .build();

        pdTracker.put(entry);

        assertEquals(idA, pdTracker.getSystem(entry, idA).systemId());
        assertEquals(idB, pdTracker.getSystem(entry, idB).systemId());
        assertEquals(idC, pdTracker.getSystem(entry, idC).systemId());
    }

    @Test
    public void shouldReturnNullWhenSystemIsMissing() throws PdStoreException {

        int entryIdA = 0;
        int entryIdB = 1;

        final String systemIdA = "Sys-A";
        final String systemIdB = "Sys-B";
        final String systemIdC = "Sys-C";

        final PdeSystemDto systemA = new PdeSystemBuilder()
            .systemId(systemIdA)
            .build();

        final PdeSystemDto systemB = new PdeSystemBuilder()
            .systemId(systemIdB)
            .build();

        final PdeSystemDto systemC = new PdeSystemBuilder()
            .systemId(systemIdC)
            .build();

        final var entryA = new PlantDescriptionEntryBuilder()
            .id(entryIdA)
            .plantDescription("A")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .systems(List.of(systemA))
            .build();

        final var entryB = new PlantDescriptionEntryBuilder()
            .id(entryIdB)
            .plantDescription("B")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .include(List.of(entryIdA))
            .systems(List.of(systemB, systemC))
            .build();

        pdTracker.put(entryA);
        pdTracker.put(entryB);
        assertNull(pdTracker.getSystem(entryB, "Nonexistent"));
    }

    @Test
    public void shouldReturnSystemFromIncludedEntry() throws PdStoreException {

        int entryIdA = 32;
        int entryIdB = 8;
        int entryIdC = 58;

        final String systemIdA = "Sys-A";
        final String systemIdB = "Sys-B";
        final String systemIdC = "Sys-C";

        final PdeSystemDto systemA = new PdeSystemBuilder()
            .systemId(systemIdA)
            .build();

        final PdeSystemDto systemB = new PdeSystemBuilder()
            .systemId(systemIdB)
            .build();

        final PdeSystemDto systemC = new PdeSystemBuilder()
            .systemId(systemIdC)
            .build();

        final var entryA = new PlantDescriptionEntryBuilder()
            .id(entryIdA)
            .plantDescription("A")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .systems(List.of(systemA, systemB))
            .build();

        final var entryB = new PlantDescriptionEntryBuilder()
            .id(entryIdB)
            .plantDescription("B")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .include(List.of(entryIdA))
            .build();

        final var entryC = new PlantDescriptionEntryBuilder()
            .id(entryIdC)
            .plantDescription("C")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .include(List.of(entryIdB))
            .systems(List.of(systemC))
            .build();

        pdTracker.put(entryA);
        pdTracker.put(entryB);
        pdTracker.put(entryC);

        assertEquals(systemIdA, pdTracker.getSystem(entryC, systemIdA).systemId());
    }

    @Test
    public void shouldReturnAllSystems() throws PdStoreException {

        int entryIdA = 32;
        int entryIdB = 8;
        int entryIdC = 58;

        final String systemIdA = "Sys-A";
        final String systemIdB = "Sys-B";
        final String systemIdC = "Sys-C";

        final PdeSystemDto systemA = new PdeSystemBuilder()
            .systemId(systemIdA)
            .build();

        final PdeSystemDto systemB = new PdeSystemBuilder()
            .systemId(systemIdB)
            .build();

        final PdeSystemDto systemC = new PdeSystemBuilder()
            .systemId(systemIdC)
            .build();

        final var entryA = new PlantDescriptionEntryBuilder()
            .id(entryIdA)
            .plantDescription("A")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .systems(List.of(systemA, systemB))
            .build();

        final var entryB = new PlantDescriptionEntryBuilder()
            .id(entryIdB)
            .plantDescription("B")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .include(List.of(entryIdA))
            .build();

        final var entryC = new PlantDescriptionEntryBuilder()
            .id(entryIdC)
            .plantDescription("C")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .include(List.of(entryIdB))
            .systems(List.of(systemC))
            .build();

        pdTracker.put(entryA);
        pdTracker.put(entryB);
        pdTracker.put(entryC);

        final var systems = pdTracker.getAllSystems(entryIdC);

        assertEquals(3, systems.size());

        var retrievedA = systems.stream()
            .filter(system -> system.systemId().equals(systemIdA))
            .findFirst()
            .get();
        var retrievedB = systems.stream()
            .filter(system -> system.systemId().equals(systemIdB))
            .findFirst()
            .get();
        var retrievedC = systems.stream()
            .filter(system -> system.systemId().equals(systemIdC))
            .findFirst()
            .orElse(null);

        assertNotNull(retrievedA);
        assertNotNull(retrievedB);
        assertNotNull(retrievedC);
    }

    @Test
    public void shouldThrowWhenGettingSystemsFromNullEntry() throws PdStoreException {
        int nonexistentId = 32;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pdTracker.getAllSystems(nonexistentId);
        });
        assertEquals(
            "Plant Description with ID " + nonexistentId + " is not present in the Plant Description Tracker.",
            exception.getMessage()
        );
    }

    @Test
    public void shouldReturnAllConnections() throws PdStoreException {

        // First entry
        int entryIdA = 0;
        String consumerIdA  = "Cons-A";
        String consumerNameA  = "Consumer A";
        String producerNameA  = "Producer A";
        String consumerPortA = "Cons-Port-A";
        String producerPortA = "Prod-Port-A";
        String producerIdA  = "Prod-A";

        final List<PortDto> consumerPortsA = List.of(
            new PortBuilder()
                .portName(consumerPortA)
                .serviceDefinition("Monitorable")
                .consumer(true)
                .build());

        final List<PortDto> producerPortsA = List.of(
            new PortBuilder()
                .portName(producerPortA)
                .serviceDefinition("Monitorable")
                .consumer(false)
                .build());

        final PdeSystemDto consumerSystemA = new PdeSystemBuilder()
            .systemId(consumerIdA)
            .systemName(consumerNameA)
            .ports(consumerPortsA)
            .build();

        final PdeSystemDto producerSystemA = new PdeSystemBuilder()
            .systemId(producerIdA)
            .systemName(producerNameA)
            .ports(producerPortsA)
            .build();

        final List<ConnectionDto> connectionsA = List.of(new ConnectionBuilder()
                .consumer(new SystemPortBuilder()
                    .systemId(consumerIdA)
                    .portName(consumerPortA)
                    .build())
                .producer(new SystemPortBuilder()
                    .systemId(producerIdA)
                    .portName(producerPortA)
                    .build())
                .build());

        final var entryA = new PlantDescriptionEntryBuilder()
            .id(entryIdA)
            .plantDescription("Plant Description A")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .systems(List.of(consumerSystemA, producerSystemA))
            .connections(connectionsA)
            .build();

        // Second entry
        int entryIdB = 1;
        String consumerIdB  = "Cons-B";
        String consumerNameB  = "Consumer B";
        String consumerPortB = "Cons-Port-B";

        final List<PortDto> consumerPortsB = List.of(
            new PortBuilder()
                .portName(consumerPortB)
                .serviceDefinition("Monitorable")
                .consumer(true)
                .build());

        final PdeSystemDto consumerSystemB = new PdeSystemBuilder()
            .systemId(consumerIdB)
            .systemName(consumerNameB)
            .ports(consumerPortsB)
            .build();

        final List<ConnectionDto> connectionsB = List.of(new ConnectionBuilder()
                .consumer(new SystemPortBuilder()
                    .systemId(consumerIdB)
                    .portName(consumerPortB)
                    .build())
                .producer(new SystemPortBuilder()
                    .systemId(producerIdA)
                    .portName(producerPortA)
                    .build())
                .build());

        final var entryB = new PlantDescriptionEntryBuilder()
            .id(entryIdB)
            .plantDescription("Plant Description B")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .include(List.of(entryIdA))
            .systems(List.of(consumerSystemB))
            .connections(connectionsB)
            .build();

        pdTracker.put(entryA);
        pdTracker.put(entryB);

        final var retrievedConnections = pdTracker.getAllConnections(entryIdB);
        assertEquals(2, retrievedConnections.size());

        final var connectionA = retrievedConnections.get(1);
        final var connectionB = retrievedConnections.get(0);

        assertEquals(consumerIdA, connectionA.consumer().systemId());
        assertEquals(consumerIdB, connectionB.consumer().systemId());
    }

    @Test
    public void shouldThrowWhenGettingConnectionsFromNullEntry() throws PdStoreException {
        int nonexistentId = 2;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pdTracker.getAllConnections(nonexistentId);
        });
        assertEquals(
            "Plant Description with ID " + nonexistentId + " is not present in the Plant Description Tracker.",
            exception.getMessage()
        );
    }

    @Test
    public void shouldReturnServiceDefinition() throws PdStoreException {

        // First entry
        int entryIdA = 0;
        String consumerIdA  = "Cons-A";
        String consumerNameA  = "Consumer A";
        String producerNameA  = "Producer A";
        String consumerPortA = "Cons-Port-A";
        String producerPortA = "Prod-Port-A";
        String producerIdA  = "Prod-A";
        String serviceDefinitionA = "SD-A";

        final List<PortDto> consumerPortsA = List.of(
            new PortBuilder()
                .portName(consumerPortA)
                .serviceDefinition("Service-XYZ")
                .consumer(true)
                .build());

        final List<PortDto> producerPortsA = List.of(
            new PortBuilder()
                .portName(producerPortA)
                .serviceDefinition(serviceDefinitionA)
                .consumer(false)
                .build());

        final PdeSystemDto consumerSystemA = new PdeSystemBuilder()
            .systemId(consumerIdA)
            .systemName(consumerNameA)
            .ports(consumerPortsA)
            .build();

        final PdeSystemDto producerSystemA = new PdeSystemBuilder()
            .systemId(producerIdA)
            .systemName(producerNameA)
            .ports(producerPortsA)
            .build();

        final var entryA = new PlantDescriptionEntryBuilder()
            .id(entryIdA)
            .plantDescription("Plant Description A")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .systems(List.of(consumerSystemA, producerSystemA))
            .build();

        // Second entry
        int entryIdB = 1;
        String consumerIdB  = "Cons-B";
        String consumerNameB  = "Consumer B";
        String consumerPortB = "Cons-Port-B";

        final List<PortDto> consumerPortsB = List.of(
            new PortBuilder()
                .portName(consumerPortB)
                .serviceDefinition("Service-ABC")
                .consumer(true)
                .build());

        final PdeSystemDto consumerSystemB = new PdeSystemBuilder()
            .systemId(consumerIdB)
            .systemName(consumerNameB)
            .ports(consumerPortsB)
            .build();

        final var entryB = new PlantDescriptionEntryBuilder()
            .id(entryIdB)
            .plantDescription("Plant Description B")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .include(List.of(entryIdA))
            .systems(List.of(consumerSystemB))
            .build();

        pdTracker.put(entryA);
        pdTracker.put(entryB);

        String serviceDefinition = pdTracker.getServiceDefinition(entryIdB, producerPortA);
        assertEquals(serviceDefinitionA, serviceDefinition);

    }

    @Test
    public void shouldThrowWhenGettingSdFromNullEntry() throws PdStoreException {
        int nonexistentId = 67;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pdTracker.getServiceDefinition(nonexistentId, "monitorable");
        });
        assertEquals(
            "Plant Description with ID " + nonexistentId + " is not present in the Plant Description Tracker.",
            exception.getMessage()
        );
    }

    @Test
    public void shouldThrowWhenGettingSdFromNonexistentPort() throws PdStoreException {
        int entryId = 98;
        String nonexistentPort = "qwerty";

        final var entry = new PlantDescriptionEntryBuilder()
            .id(entryId)
            .plantDescription("Plant Description A")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .build();

        pdTracker.put(entry);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pdTracker.getServiceDefinition(entryId, nonexistentPort);
        });
        assertEquals(
            "No port named '" + nonexistentPort + "' could be found in the Plant Description Tracker.",
            exception.getMessage()
        );
    }


}
