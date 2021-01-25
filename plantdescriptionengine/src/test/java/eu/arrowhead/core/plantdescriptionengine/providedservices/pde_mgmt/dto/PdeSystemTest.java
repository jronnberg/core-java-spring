package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;


public class PdeSystemTest {

    final Instant now = Instant.now();

    @Test
    public void shouldReturnDeactivatedCopy() {

        final String portNameA = "port_a";
        final String portNameB = "port_b";
        final String portNameC = "port_c";

        final String serviceA = "ser-a";
        final String serviceB = "ser-b";
        final String serviceC = "ser-c";

        final var metadataA = Map.of("x", "1");
        final var metadataB = Map.of("y", "2");
        final var metadataC = Map.of("z", "3");

        boolean isConsumerA = true;
        boolean isConsumerB = false;
        boolean isConsumerC = true;

        final List<PortDto> ports = List.of(
            new PortBuilder()
                .portName(portNameA)
                .serviceDefinition(serviceA)
                .consumer(isConsumerA)
                .metadata(metadataA)
                .build(),
            new PortBuilder()
                .portName(portNameB)
                .serviceDefinition(serviceB)
                .metadata(metadataB)
                .consumer(isConsumerB)
                .build(),
            new PortBuilder()
                .portName(portNameC)
                .serviceDefinition(serviceC)
                .metadata(metadataC)
                .consumer(isConsumerC)
                .build()
        );

        final PdeSystemDto system = new PdeSystemBuilder()
            .systemId("Sys-X")
            .ports(ports)
            .build();

        final var portA = system.getPort(portNameA);
        final var portB = system.getPort(portNameB);
        final var nullPort = system.getPort("Nonexistent");

        assertEquals(portNameA, portA.portName());
        assertEquals(portNameB, portB.portName());

        assertEquals(isConsumerA, portA.consumer().get());
        assertEquals(isConsumerB, portB.consumer().get());

        assertEquals(metadataA, portA.metadata().get());
        assertEquals(metadataB, portB.metadata().get());

        assertEquals(serviceA, portA.serviceDefinition());
        assertEquals(serviceB, portB.serviceDefinition());

        assertNull(nullPort);
    }
}
