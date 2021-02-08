package eu.arrowhead.core.plantdescriptionengine.orchestratorclient;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.CloudBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.CloudDto;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreRule;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.InMemoryRuleStore;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.RuleStore;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.RuleStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
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
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.ServiceDefinitionBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.ServiceInterfaceBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystemDto;
import eu.arrowhead.core.plantdescriptionengine.utils.MockClientResponse;
import eu.arrowhead.core.plantdescriptionengine.utils.MockSystemTracker;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.util.concurrent.Future;

@ExtendWith(MockitoExtension.class)
public class OrchestratorClientTest {

    final String serviceDefinitionA = "service_a";

    Instant now = Instant.now();

    private SrSystemDto createConsumerSystem(String systemName) {
        return new SrSystemBuilder().id(0).systemName(systemName).address("0.0.0.1").port(3001).authenticationInfo(null)
                .createdAt(now.toString()).updatedAt(now.toString()).build();
    }

    private SrSystemDto createProviderSystem(String systemName) {
        return new SrSystemBuilder().id(1).systemName(systemName).address("0.0.0.2").port(5001).authenticationInfo(null)
                .createdAt(now.toString()).updatedAt(now.toString()).build();
    }

    private SrSystemDto createOrchestratorSystem() {
        return new SrSystemBuilder().id(2).systemName("orchestrator").address("0.0.0.3").port(5002)
                .authenticationInfo(null).createdAt(now.toString()).updatedAt(now.toString()).build();
    }

    /**
     * @return A mock Plant Description entry.
     */
    private PlantDescriptionEntryDto createEntry() {
        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";

        final List<PortDto> consumerPorts = List.of(
                new PortBuilder().portName(consumerPort).serviceDefinition(serviceDefinitionA).consumer(true).build());

        final List<PortDto> producerPorts = List.of(
                new PortBuilder().portName(producerPort).serviceDefinition(serviceDefinitionA).consumer(false).build());

        final PdeSystemDto consumerSystem = new PdeSystemBuilder().systemId(consumerId).systemName("Consumer A")
                .ports(consumerPorts).build();

        final PdeSystemDto producerSystem = new PdeSystemBuilder().systemId(producerId).systemName("Producer A")
                .ports(producerPorts).build();

        final List<ConnectionDto> connections = List.of(new ConnectionBuilder()
                .consumer(new SystemPortBuilder().systemId(consumerId).portName(consumerPort).build())
                .producer(new SystemPortBuilder().systemId(producerId).portName(producerPort).build()).build());
        return new PlantDescriptionEntryBuilder().id(0).plantDescription("Plant Description 1A").createdAt(now)
                .updatedAt(now).active(true).include(new ArrayList<>()).systems(List.of(consumerSystem, producerSystem))
                .connections(connections).build();
    }

    @Test
    public void shouldCreateRule() throws SSLException, RuleStoreException {
        final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final CloudDto cloud = new CloudBuilder().name("Cloud_a").operator("Operator_a").build();

        MockSystemTracker systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));

        final PlantDescriptionEntry entry = createEntry();

        var consumerSystem = entry.systems().get(0);
        var providerSystem = entry.systems().get(1);

        var consumerSrSystem = createConsumerSystem(consumerSystem.systemName().get());
        var providerSrSystem = createProviderSystem(providerSystem.systemName().get());
        var orchestratorSrSystem = createOrchestratorSystem();

        systemTracker.addSystem(consumerSrSystem);
        systemTracker.addSystem(providerSrSystem);
        systemTracker.addSystem(orchestratorSrSystem);

        final RuleStore backingStore = new InMemoryRuleStore();
        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, backingStore, systemTracker);

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
    public void shouldStoreRule() throws SSLException, RuleStoreException {

        // final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final HttpClient httpClient = Mockito.mock(HttpClient.class);
        final var systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));
        final PlantDescriptionEntry entry = createEntry();

        var consumerSystem = entry.systems().get(0);
        var providerSystem = entry.systems().get(1);

        var consumerSrSystem = createConsumerSystem(consumerSystem.systemName().get());
        var providerSrSystem = createProviderSystem(providerSystem.systemName().get());
        var orchestratorSrSystem = createOrchestratorSystem();

        systemTracker.addSystem(consumerSrSystem);
        systemTracker.addSystem(providerSrSystem);
        systemTracker.addSystem(orchestratorSrSystem);

        final CloudDto cloud = new CloudBuilder().name("Cloud_a").operator("Operator_a").build();
        final RuleStore backingStore = new InMemoryRuleStore();
        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, backingStore, systemTracker);

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse response = new MockClientResponse();
        int ruleId = 39;
        response.status(HttpStatus.CREATED);
        response.body(new StoreEntryListBuilder().count(1)
                .data(new StoreEntryBuilder().id(ruleId).foreign(false)
                    .serviceDefinition(new ServiceDefinitionBuilder().serviceDefinition(serviceDefinitionA).build())
                    .providerSystem(providerSrSystem).consumerSystem(consumerSrSystem)
                    .serviceInterface(new ServiceInterfaceBuilder().id(177).interfaceName("HTTP_INSECURE_JSON")
                            .createdAt(now.toString()).updatedAt(now.toString()).build())
                    .priority(1).createdAt(now.toString()).updatedAt(now.toString()).build())
            .build());

        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
                .thenReturn(Future.success(response));

        // This is the heart of the test, the method being investigated:
        orchestratorClient.onPlantDescriptionAdded(entry);

        // Verify that the rule was stored correctly:
        assertTrue(backingStore.readRules().contains(ruleId));

        // Verify that the HTTP client was passed correct data:
        ArgumentCaptor<InetSocketAddress> addressCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
        ArgumentCaptor<HttpClientRequest> requestCaptor = ArgumentCaptor.forClass(HttpClientRequest.class);

        verify(httpClient).send(addressCaptor.capture(), requestCaptor.capture());

        InetSocketAddress capturedAddress = addressCaptor.getValue();
        HttpClientRequest capturedRequest = requestCaptor.getValue();

        assertEquals(orchestratorSrSystem.address(), capturedAddress.getAddress().getHostAddress());
        assertEquals(orchestratorSrSystem.port(), capturedAddress.getPort());

        assertEquals(HttpMethod.POST, capturedRequest.method().get());
        assertEquals("/orchestrator/mgmt/store", capturedRequest.uri().get());
        assertEquals(orchestratorSrSystem.address(), capturedAddress.getAddress().getHostAddress());
        assertEquals(orchestratorSrSystem.port(), capturedAddress.getPort());

        @SuppressWarnings("unchecked")
        List<StoreRule> rulesSent = (List<StoreRule>) capturedRequest.body().get();
        assertEquals(1, rulesSent.size());
        StoreRule ruleSent = rulesSent.get(0);
        assertEquals(1, ruleSent.priority());
        assertEquals(serviceDefinitionA, ruleSent.serviceDefinitionName());
        assertEquals(providerSrSystem.systemName(), ruleSent.providerSystem().systemName());
        assertEquals(providerSrSystem.address(), ruleSent.providerSystem().address());
        assertEquals(providerSrSystem.port(), ruleSent.providerSystem().port());
        assertEquals(consumerSrSystem.id(), ruleSent.consumerSystemId());
        assertEquals(cloud.name(), ruleSent.cloud().name());
        assertEquals(cloud.operator(), ruleSent.cloud().operator());
    }

    @Test
    public void shouldRemoveRule() throws SSLException, RuleStoreException, PdStoreException {

        // final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final HttpClient httpClient = Mockito.mock(HttpClient.class);
        final var systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));
        final PlantDescriptionEntryDto activeEntry = createEntry();

        final var pdStore = new InMemoryPdStore();
        pdStore.write(activeEntry);
        final var pdTracker = new PlantDescriptionTracker(pdStore);

        var consumerSystem = activeEntry.systems().get(0);
        var providerSystem = activeEntry.systems().get(1);

        var consumerSrSystem = createConsumerSystem(consumerSystem.systemName().get());
        var providerSrSystem = createProviderSystem(providerSystem.systemName().get());
        var orchestratorSrSystem = createOrchestratorSystem();

        systemTracker.addSystem(consumerSrSystem);
        systemTracker.addSystem(providerSrSystem);
        systemTracker.addSystem(orchestratorSrSystem);

        int ruleId = 65;

        final RuleStore ruleStore = new InMemoryRuleStore();
        ruleStore.setRules(Set.of(ruleId));

        final CloudDto cloud = new CloudBuilder()
            .name("Cloud_a")
            .operator("Operator_a")
            .build();

        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, ruleStore, systemTracker);

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse deletionResponse = new MockClientResponse();
        deletionResponse.status(HttpStatus.OK);

        final MockClientResponse creationResponse = new MockClientResponse();
        int newRuleId = 82;
        creationResponse.status(HttpStatus.CREATED);
        creationResponse.body(new StoreEntryListBuilder().count(1)
                .data(new StoreEntryBuilder().id(newRuleId).foreign(false)
                    .serviceDefinition(new ServiceDefinitionBuilder().serviceDefinition(serviceDefinitionA).build())
                    .providerSystem(providerSrSystem).consumerSystem(consumerSrSystem)
                    .serviceInterface(new ServiceInterfaceBuilder().id(177).interfaceName("HTTP_INSECURE_JSON")
                            .createdAt(now.toString()).updatedAt(now.toString()).build())
                    .priority(1).createdAt(now.toString()).updatedAt(now.toString()).build())
            .build());

        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
            .thenReturn(
                Future.success(deletionResponse),
                Future.success(creationResponse)
            );

        orchestratorClient.initialize(pdTracker)
            .ifSuccess(result -> {
                assertEquals(1, ruleStore.readRules().size());
                orchestratorClient.onPlantDescriptionRemoved(activeEntry);
                assertTrue(ruleStore.readRules().isEmpty());
            }).onFailure(e -> {
                assertNull(e);
            });
    }

    @Test
    public void shouldNotRemoveRule() throws SSLException, RuleStoreException, PdStoreException {

        // final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final HttpClient httpClient = Mockito.mock(HttpClient.class);
        final var systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));
        final PlantDescriptionEntryDto entryA = createEntry();
        final PlantDescriptionEntryDto entryB = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description B")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .build();

        final var pdStore = new InMemoryPdStore();
        pdStore.write(entryA);
        pdStore.write(entryB);

        final var pdTracker = new PlantDescriptionTracker(pdStore);

        var consumerSystem = entryA.systems().get(0);
        var providerSystem = entryA.systems().get(1);

        var consumerSrSystem = createConsumerSystem(consumerSystem.systemName().get());
        var providerSrSystem = createProviderSystem(providerSystem.systemName().get());
        var orchestratorSrSystem = createOrchestratorSystem();

        systemTracker.addSystem(consumerSrSystem);
        systemTracker.addSystem(providerSrSystem);
        systemTracker.addSystem(orchestratorSrSystem);

        int ruleId = 65;

        final RuleStore ruleStore = new InMemoryRuleStore();
        ruleStore.setRules(Set.of(ruleId));

        final CloudDto cloud = new CloudBuilder()
            .name("Cloud_a")
            .operator("Operator_a")
            .build();

        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, ruleStore, systemTracker);

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse deletionResponse = new MockClientResponse();
        deletionResponse.status(HttpStatus.OK);

        final MockClientResponse creationResponse = new MockClientResponse();
        int newRuleId = 2;
        creationResponse.status(HttpStatus.CREATED);
        creationResponse.body(new StoreEntryListBuilder().count(1)
                .data(new StoreEntryBuilder().id(newRuleId).foreign(false)
                    .serviceDefinition(new ServiceDefinitionBuilder().serviceDefinition(serviceDefinitionA).build())
                    .providerSystem(providerSrSystem).consumerSystem(consumerSrSystem)
                    .serviceInterface(new ServiceInterfaceBuilder().id(177).interfaceName("HTTP_INSECURE_JSON")
                            .createdAt(now.toString()).updatedAt(now.toString()).build())
                    .priority(1).createdAt(now.toString()).updatedAt(now.toString()).build())
            .build());

        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
            .thenReturn(
                Future.success(deletionResponse),
                Future.success(creationResponse)
            );

        orchestratorClient.initialize(pdTracker)
            .ifSuccess(result -> {
                assertEquals(1, ruleStore.readRules().size());
                orchestratorClient.onPlantDescriptionRemoved(entryB);
                assertEquals(1, ruleStore.readRules().size());
            }).onFailure(e -> {
                assertNull(e);
            });
    }

    @Test
    public void shouldHandleRemovalOfInactivePd() throws SSLException, RuleStoreException, PdStoreException {

        final HttpClient httpClient = Mockito.mock(HttpClient.class);
        final var systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));
        final PlantDescriptionEntryDto inactiveEntry = PlantDescriptionEntry.deactivated(createEntry());

        final var pdStore = new InMemoryPdStore();
        pdStore.write(inactiveEntry);

        final var pdTracker = new PlantDescriptionTracker(pdStore);

        var consumerSystem = inactiveEntry.systems().get(0);
        var providerSystem = inactiveEntry.systems().get(1);

        var consumerSrSystem = createConsumerSystem(consumerSystem.systemName().get());
        var providerSrSystem = createProviderSystem(providerSystem.systemName().get());
        var orchestratorSrSystem = createOrchestratorSystem();

        systemTracker.addSystem(consumerSrSystem);
        systemTracker.addSystem(providerSrSystem);
        systemTracker.addSystem(orchestratorSrSystem);

        final RuleStore ruleStore = new InMemoryRuleStore();
        final CloudDto cloud = new CloudBuilder()
            .name("Cloud_a")
            .operator("Operator_a")
            .build();

        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, ruleStore, systemTracker);
        orchestratorClient.initialize(pdTracker)
            .ifSuccess(result -> {
                assertTrue(ruleStore.readRules().isEmpty());
                orchestratorClient.onPlantDescriptionRemoved(inactiveEntry);
                assertTrue(ruleStore.readRules().isEmpty());
            }).onFailure(e -> {
                assertNull(e);
            });
    }
}