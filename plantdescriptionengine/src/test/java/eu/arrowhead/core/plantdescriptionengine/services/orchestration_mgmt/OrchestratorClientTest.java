package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.alarmmanager.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.CloudDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulebackingstore.InMemoryRuleStore;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulebackingstore.RuleStore;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulebackingstore.RuleStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.ConnectionBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.ConnectionDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PortBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PortDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.SystemPortBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import eu.arrowhead.core.plantdescriptionengine.utils.MockSystemTracker;
import se.arkalix.net.http.client.HttpClient;

public class OrchestratorClientTest {

    /**
     * @return A mock Plant Description entry.
     */
    private PlantDescriptionEntry createEntry() {
        final String consumerId = "system_1";
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
            .systemName("Consumer A")
            .ports(consumerPorts)
            .build();

        final PdeSystemDto producerSystem = new PdeSystemBuilder()
            .systemId(producerId)
            .systemName("Producer A")
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

    @Test
    public void shouldCreateRule() throws SSLException, RuleStoreException {
        final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final CloudDto cloud = new CloudBuilder()
            .name("Cloud_a")
            .operator("Operator_a")
            .build();

        MockSystemTracker systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));
        Locator.setSystemTracker(systemTracker);

        final PlantDescriptionEntry entry = createEntry();

        var consumerSystem = entry.systems().get(0);
        var providerSystem = entry.systems().get(1);

        var consumerSrSystem = new SrSystemBuilder()
            .id(0)
            .systemName(consumerSystem.systemName().get())
            .address("0.0.0.0")
            .port(5000)
            .authenticationInfo(null)
            .createdAt(Instant.now().toString())
            .updatedAt(Instant.now().toString())
            .build();

        var providerSrSystem = new SrSystemBuilder()
            .id(1)
            .systemName(providerSystem.systemName().get())
            .address("0.0.0.0")
            .port(5001)
            .authenticationInfo(null)
            .createdAt(Instant.now().toString())
            .updatedAt(Instant.now().toString())
            .build();

        var orchestratorSrSystem = new SrSystemBuilder()
            .id(2)
            .systemName("orchestrator")
            .address("0.0.0.0")
            .port(5002)
            .authenticationInfo(null)
            .createdAt(Instant.now().toString())
            .updatedAt(Instant.now().toString())
            .build();

        systemTracker.addSystem(consumerSystem.systemName().get(), consumerSrSystem);
        systemTracker.addSystem(providerSystem.systemName().get(), providerSrSystem);
        systemTracker.addSystem("orchestrator", orchestratorSrSystem);

        final RuleStore backingStore = new InMemoryRuleStore();
        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, backingStore);

        var rule = orchestratorClient.createRule(entry, 0);

        assertEquals(cloud.name(), rule.cloud().name());
        assertEquals(cloud.operator(), rule.cloud().operator());
        assertEquals(consumerSrSystem.id(), rule.consumerSystemId());
        assertEquals(providerSrSystem.systemName(), rule.providerSystem().systemName());
        assertEquals(providerSystem.ports().get(0).serviceDefinition(), rule.serviceDefinitionName());
        assertEquals(cloud.name(), rule.cloud().name());
        assertEquals(cloud.operator(), rule.cloud().operator());
    }

    @Test
    public void shouldRaiseMissingSystemAlarms() throws SSLException, RuleStoreException {
        final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final CloudDto cloud = new CloudBuilder()
            .name("Cloud_a")
            .operator("Operator_a")
            .build();

        MockSystemTracker systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));
        Locator.setSystemTracker(systemTracker);

        final var alarmManager = new AlarmManager();
        Locator.setAlarmManager(alarmManager);

        final PlantDescriptionEntry entry = createEntry();

        var consumerSystem = entry.systems().get(0);
        var providerSystem = entry.systems().get(1);

        var orchestratorSrSystem = new SrSystemBuilder()
            .id(2)
            .systemName("orchestrator")
            .address("0.0.0.0")
            .port(5002)
            .authenticationInfo(null)
            .createdAt(Instant.now().toString())
            .updatedAt(Instant.now().toString())
            .build();

        systemTracker.addSystem("orchestrator", orchestratorSrSystem);

        final RuleStore backingStore = new InMemoryRuleStore();
        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, backingStore);

        // No rule should have been created, since the systems are missing.
        var rule = orchestratorClient.createRule(entry, 0);
        assertNull(rule);

        final var alarms = alarmManager.getAlarmList();
        assertEquals(2, alarms.count());
        boolean hasMissingConsumerAlarm = false;
        boolean hasMissingProducerAlarm = false;

        for (PdeAlarm alarm : alarms.data()) {
            System.out.println(alarm.description());
            String missingConsumerString = "System with ID '" + consumerSystem.systemId() + "' cannot be found in the Service Registry.";
            String missingProducerString = "System with ID '" + providerSystem.systemId() + "' cannot be found in the Service Registry.";
            if (alarm.description().equals(missingConsumerString)) {
                    hasMissingConsumerAlarm = true;
            }
            if (alarm.description().equals(missingProducerString)) {
                hasMissingProducerAlarm = true;
            }
        }
        assertTrue(hasMissingConsumerAlarm);
        assertTrue(hasMissingProducerAlarm);
    }

}