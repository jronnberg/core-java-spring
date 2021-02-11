package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.ConnectionBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.ConnectionDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.SystemPortBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;


public class PlantDescriptionValidatorTest {

    final Instant now = Instant.now();

    PdStore store;
    PlantDescriptionTracker pdTracker;

    @BeforeEach
    public void initEach() throws PdStoreException {
        store = new InMemoryPdStore();
        pdTracker = new PlantDescriptionTracker(store);
    }

    @Test
    public void shouldNotReportErrors() throws PdStoreException {

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

        final var validator = new PlantDescriptionValidator(entryB, pdTracker);
        System.out.println(validator.getErrorMessage());
        assertFalse(validator.hasError());
    }


    @Test
    public void shouldReportDuplicatePorts() throws PdStoreException {

        final String systemId = "system_a";
        final String portName = "port_a";

        final List<PortDto> consumerPorts = List.of(
            new PortBuilder()
                .portName(portName)
                .serviceDefinition("service_a")
                .consumer(true)
                .build(),
            new PortBuilder()
                .portName(portName)
                .serviceDefinition("service_b")
                .consumer(true)
                .build()
        );

        final PdeSystemDto consumerSystem = new PdeSystemBuilder()
            .systemId(systemId)
            .ports(consumerPorts)
            .build();

        final var entry = new PlantDescriptionEntryBuilder()
            .plantDescription("Plant Description 1A")
            .id(123)
            .active(true)
            .createdAt(now)
            .updatedAt(now)
            .systems(List.of(consumerSystem))
            .build();

        final var validator = new PlantDescriptionValidator(entry, pdTracker);
        assertTrue(validator.hasError());

        String expectedErrorMessage = "<Duplicate port name '" +
            portName + "' in system '" + systemId + "'>";
        assertEquals(expectedErrorMessage, validator.getErrorMessage());
    }

    @Test
    public void shouldRequireMetadataToDifferentiateBetweenPorts() throws PdStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPortA = "consumerPortA";
        final String producerPort = "port_2";
        final String serviceDefinition = "service_a";

        final List<PortDto> consumerPorts = List.of(
            new PortBuilder()
                .portName(consumerPortA)
                .serviceDefinition(serviceDefinition)
                .metadata(Map.of("a", "1"))
                .consumer(true)
                .build(),
            new PortBuilder()
                .portName("port_b")
                .serviceDefinition(serviceDefinition)
                .consumer(true)
                .build(),
            new PortBuilder()
                .portName("port_c")
                .serviceDefinition(serviceDefinition)
                .consumer(true)
                .build()
        );

        final List<PortDto> producerPorts = List.of(
            new PortBuilder()
                .portName(producerPort)
                .serviceDefinition(serviceDefinition)
                .consumer(false)
                .build()
        );

        final PdeSystemDto consumerSystem = new PdeSystemBuilder()
            .systemId(consumerId)
            .ports(consumerPorts)
            .build();

        final PdeSystemDto producerSystem = new PdeSystemBuilder()
            .systemId(producerId)
            .ports(producerPorts)
            .build();

        final List<ConnectionDto> connections = List.of(
            new ConnectionBuilder()
                .consumer(new SystemPortBuilder()
                    .systemId(consumerId)
                    .portName(consumerPortA)
                    .build())
                .producer(new SystemPortBuilder()
                    .systemId(producerId)
                    .portName(producerPort)
                    .build())
                .build()
        );
        final var entry = new PlantDescriptionEntryBuilder()
            .id(789)
            .plantDescription("Plant Description 1A")
            .active(true)
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .createdAt(now)
            .updatedAt(now)
            .build();

        final var validator = new PlantDescriptionValidator(entry, pdTracker);
        assertTrue(validator.hasError());

        String expectedErrorMessage = "<" + consumerId + " has multiple ports with service definition '" +
            serviceDefinition + "' without metadata.>";
        assertEquals(expectedErrorMessage, validator.getErrorMessage());
    }

    @Test
    public void shouldReportInvalidProducerPort() throws PdStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String invalidPort = "no_such_port";
        final String serviceDefinition = "service_a";

        final List<PortDto> consumerPorts = List.of(
            new PortBuilder()
                .portName(consumerPort)
                .serviceDefinition(serviceDefinition)
                .consumer(true)
                .build()
        );

        final List<PortDto> producerPorts = List.of(
            new PortBuilder()
                .portName(producerPort)
                .serviceDefinition(serviceDefinition)
                .consumer(false)
                .build()
        );

        final PdeSystemDto consumerSystem = new PdeSystemBuilder()
            .systemId(consumerId)
            .ports(consumerPorts)
            .build();

        final PdeSystemDto producerSystem = new PdeSystemBuilder()
            .systemId(producerId)
            .ports(producerPorts)
            .build();

        final List<ConnectionDto> connections = List.of(
            new ConnectionBuilder()
                .consumer(new SystemPortBuilder()
                    .systemId(consumerId)
                    .portName(consumerPort)
                    .build())
                .producer(new SystemPortBuilder()
                    .systemId(producerId)
                    .portName(invalidPort)
                    .build())
                .build()
        );
        final var entry = new PlantDescriptionEntryBuilder()
            .id(42)
            .plantDescription("Plant Description 1A")
            .active(true)
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .createdAt(now)
            .updatedAt(now)
            .build();

        final var validator = new PlantDescriptionValidator(entry, pdTracker);
        assertTrue(validator.hasError());

        String expectedErrorMessage = "<Connection refers to the missing producer port '" +
            invalidPort + "'>";
        assertEquals(expectedErrorMessage, validator.getErrorMessage());
    }

    @Test
    public void shouldReportInvalidConsumerPort() throws PdStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String invalidPort = "no_such_port";
        final String serviceDefinition = "service_a";

        final List<PortDto> consumerPorts = List.of(
            new PortBuilder()
                .portName(consumerPort)
                .serviceDefinition(serviceDefinition)
                .consumer(true)
                .build()
        );

        final List<PortDto> producerPorts = List.of(
            new PortBuilder()
                .portName(producerPort)
                .serviceDefinition(serviceDefinition)
                .consumer(false)
                .build()
        );

        final PdeSystemDto consumerSystem = new PdeSystemBuilder()
            .systemId(consumerId)
            .ports(consumerPorts)
            .build();

        final PdeSystemDto producerSystem = new PdeSystemBuilder()
            .systemId(producerId)
            .ports(producerPorts)
            .build();

        final List<ConnectionDto> connections = List.of(
            new ConnectionBuilder()
                .consumer(new SystemPortBuilder()
                    .systemId(consumerId)
                    .portName(invalidPort)
                    .build())
                .producer(new SystemPortBuilder()
                    .systemId(producerId)
                    .portName(producerPort)
                    .build())
                .build()
        );
        final var entry = new PlantDescriptionEntryBuilder()
            .id(89)
            .plantDescription("Plant Description 1A")
            .active(true)
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .createdAt(now)
            .updatedAt(now)
            .build();

        final var validator = new PlantDescriptionValidator(entry, pdTracker);
        assertTrue(validator.hasError());

        String expectedErrorMessage = "<Connection refers to the missing consumer port '" +
            invalidPort + "'>";
        assertEquals(expectedErrorMessage, validator.getErrorMessage());
    }

    @Test
    public void shouldReportMissingConsumer() throws PdStoreException {

      final String consumerId = "system_1";
        final String producerId = "system_2";
        final String missingId = "garbage_string";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String serviceDefinition = "service_a";

        final List<PortDto> consumerPorts = List.of(
            new PortBuilder()
                .portName(consumerPort)
                .serviceDefinition(serviceDefinition)
                .consumer(true)
                .build()
        );

        final List<PortDto> producerPorts = List.of(
            new PortBuilder()
                .portName(producerPort)
                .serviceDefinition(serviceDefinition)
                .consumer(false)
                .build()
        );

        final PdeSystemDto consumerSystem = new PdeSystemBuilder()
            .systemId(consumerId)
            .ports(consumerPorts)
            .build();

        final PdeSystemDto producerSystem = new PdeSystemBuilder()
            .systemId(producerId)
            .ports(producerPorts)
            .build();

        final List<ConnectionDto> connections = List.of(
            new ConnectionBuilder()
                .consumer(new SystemPortBuilder()
                    .systemId(missingId)
                    .portName(consumerPort)
                    .build())
                .producer(new SystemPortBuilder()
                    .systemId(producerId)
                    .portName(producerPort)
                    .build())
                .build()
        );
        final var entry = new PlantDescriptionEntryBuilder()
            .id(23)
            .plantDescription("Plant Description 1A")
            .active(true)
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .createdAt(now)
            .updatedAt(now)
            .build();

        final var validator = new PlantDescriptionValidator(entry, pdTracker);
        assertTrue(validator.hasError());

        String expectedErrorMessage = "<A connection refers to the missing system '" + missingId + "'>";
        assertEquals(expectedErrorMessage, validator.getErrorMessage());
    }

    @Test
    public void shouldReportMissingProvider() throws PdStoreException {

      final String consumerId = "system_1";
        final String producerId = "system_2";
        final String missingId = "garbage_string";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String serviceDefinition = "service_a";

        final List<PortDto> consumerPorts = List.of(
            new PortBuilder()
                .portName(consumerPort)
                .serviceDefinition(serviceDefinition)
                .consumer(true)
                .build()
        );

        final List<PortDto> producerPorts = List.of(
            new PortBuilder()
                .portName(producerPort)
                .serviceDefinition(serviceDefinition)
                .consumer(false)
                .build()
        );

        final PdeSystemDto consumerSystem = new PdeSystemBuilder()
            .systemId(consumerId)
            .ports(consumerPorts)
            .build();

        final PdeSystemDto producerSystem = new PdeSystemBuilder()
            .systemId(producerId)
            .ports(producerPorts)
            .build();

        final List<ConnectionDto> connections = List.of(
            new ConnectionBuilder()
                .consumer(new SystemPortBuilder()
                    .systemId(consumerId)
                    .portName(consumerPort)
                    .build())
                .producer(new SystemPortBuilder()
                    .systemId(missingId)
                    .portName(producerPort)
                    .build())
                .build()
        );
        final var entry = new PlantDescriptionEntryBuilder()
            .id(23)
            .plantDescription("Plant Description 1A")
            .active(true)
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .createdAt(now)
            .updatedAt(now)
            .build();

        final var validator = new PlantDescriptionValidator(entry, pdTracker);
        assertTrue(validator.hasError());

        String expectedErrorMessage = "<A connection refers to the missing system '" + missingId + "'>";
        assertEquals(expectedErrorMessage, validator.getErrorMessage());
    }

    @Test
    public void shouldReportDuplicateInclusions() throws PdStoreException {

        int entryIdA = 0;
        int entryIdB = 1;
        int entryIdC = 2;

        final var entryA = new PlantDescriptionEntryBuilder()
            .id(entryIdA)
            .plantDescription("Plant Description A")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .build();

        final var entryB = new PlantDescriptionEntryBuilder()
            .id(entryIdB)
            .plantDescription("Plant Description B")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .build();

        final var entryC = new PlantDescriptionEntryBuilder()
            .id(entryIdC)
            .plantDescription("Plant Description B")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .include(List.of(entryIdA, entryIdA, entryIdB, entryIdB))
            .build();

        pdTracker.put(entryA);
        pdTracker.put(entryB);

        final var validator = new PlantDescriptionValidator(entryC, pdTracker);
        assertTrue(validator.hasError());

        String expectedErrorMessage = "<Entry with ID '" + entryIdA + "' is included more than once.>, "
            + "<Entry with ID '" + entryIdB + "' is included more than once.>";
        assertEquals(expectedErrorMessage, validator.getErrorMessage());
    }


}
