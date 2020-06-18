package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.ConnectionBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.ConnectionDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PortBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PortDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.SystemPortBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.SystemTracker;
import eu.arrowhead.core.plantdescriptionengine.utils.MockSystemTracker;
import se.arkalix.net.http.client.HttpClient;

public class OrchestratorClientTest {

    @Test
    public void shouldCreateRule() throws SSLException {
        final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final CloudDto cloud = new CloudBuilder()
            .name("Cloud_a")
            .operator("Operator_a")
            .build();
        SystemTracker systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));
        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, systemTracker);

        final PlantDescriptionEntry entry = createEntry();
        var rule = orchestratorClient.createRule(entry, 0);

        assertEquals(cloud.name(), rule.cloud().name());
        assertEquals(cloud.operator(), rule.cloud().operator());
        // TODO: Compare attributes and metadata
    }

    private PlantDescriptionEntry createEntry() {
        final String consumerId = "system_1"
        final String producerId = "system_2";
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
                    .systemId(producerId)
                    .portName(producerPort)
                    .build())
                .build()
        );
        return new PlantDescriptionEntryBuilder()
            .id(0)
            .plantDescription("Plant Description 1A")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .active(true)
            .include(new ArrayList<>())
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .build();
    }

}