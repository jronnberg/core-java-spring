package eu.arrowhead.core.plantdescriptionengine.orchestratorclient;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.CloudBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.CloudDto;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntryDto;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntryList;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreRule;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.InMemoryRuleStore;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.RuleStore;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.RuleStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStore;
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

    final Instant now = Instant.now();

    final String consumerId = "system_1";
    final String producerId = "system_2";

    final String consumerName = "System 1";
    final String producerName = "System 2";

    final String consumerPort = "port_1";
    final String producerPort = "port_2";


    private PlantDescriptionTracker pdTracker;
    private HttpClient httpClient;
    private MockSystemTracker systemTracker;
    private RuleStore ruleStore;
    private OrchestratorClient orchestratorClient;

    final List<PortDto> consumerPorts = List.of(
        new PortBuilder()
            .portName(consumerPort)
            .serviceDefinition(serviceDefinitionA)
            .consumer(true)
            .build());

    final List<PortDto> producerPorts = List.of(
        new PortBuilder()
            .portName(producerPort)
            .serviceDefinition(serviceDefinitionA)
            .consumer(false)
            .build());

    final PdeSystemDto consumerSystem = new PdeSystemBuilder()
        .systemId(consumerId)
        .systemName(consumerName)
        .ports(consumerPorts)
        .build();

    final PdeSystemDto producerSystem = new PdeSystemBuilder()
        .systemId(producerId)
        .systemName(producerName)
        .ports(producerPorts)
        .build();

    private final SrSystemDto consumerSrSystem = new SrSystemBuilder()
        .id(1)
        .systemName(consumerName)
        .address("0.0.0.6")
        .port(5002)
        .authenticationInfo(null)
        .createdAt(now.toString())
        .updatedAt(now.toString())
        .build();

    private final SrSystemDto producerSrSystem = new SrSystemBuilder()
        .id(2)
        .systemName(producerName)
        .address("0.0.0.7")
        .port(5003)
        .authenticationInfo(null)
        .createdAt(now.toString())
        .updatedAt(now.toString())
        .build();

    private final SrSystemDto orchestratorSrSystem = new SrSystemBuilder()
        .id(0)
        .systemName("orchestrator")
        .address("0.0.0.5")
        .port(5001)
        .authenticationInfo(null)
        .createdAt(now.toString())
        .updatedAt(now.toString())
        .build();

    private final CloudDto cloud = new CloudBuilder()
        .name("Cloud_a")
        .operator("Operator_a")
        .build();

    private StoreEntryDto createStoreEntryRule(int ruleId, SrSystemDto provider, SrSystemDto consumer) {
        return new StoreEntryBuilder()
            .id(ruleId)
            .foreign(false)
            .providerSystem(provider)
            .consumerSystem(consumer)
            .priority(1)
            .createdAt(now.toString())
            .updatedAt(now.toString())
            .serviceInterface(new ServiceInterfaceBuilder()
                .id(177).interfaceName("HTTP_INSECURE_JSON")
                .createdAt(now.toString())
                .updatedAt(now.toString())
                .build())
            .serviceDefinition(new ServiceDefinitionBuilder()
                .serviceDefinition(serviceDefinitionA)
                .build())
            .build();
    }

    private StoreEntryList createSingleRuleStoreList(int ruleId, SrSystemDto provider, SrSystemDto consumer) {
        return new StoreEntryListBuilder()
            .count(1)
            .data(List.of(createStoreEntryRule(ruleId, provider, consumer)))
            .build();
    }

    /**
     * @return A mock Plant Description entry.
     */
    private PlantDescriptionEntryDto createEntry() {

        final List<ConnectionDto> connections = List.of(new ConnectionBuilder()
            .consumer(new SystemPortBuilder()
                .systemId(consumerId)
                .portName(consumerPort)
                .build())
            .producer(new SystemPortBuilder()
                .systemId(producerId)
                .portName(producerPort)
                .build())
            .build());

        return new PlantDescriptionEntryBuilder()
            .id(0).plantDescription("Plant Description 1A")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .include(new ArrayList<>())
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections).build();
    }

    @BeforeEach
    public void initEach() throws PdStoreException {
        PdStore pdStore = new InMemoryPdStore();
        httpClient = Mockito.mock(HttpClient.class);
        pdTracker = new PlantDescriptionTracker(pdStore);
        systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));

        systemTracker.addSystem(consumerSrSystem);
        systemTracker.addSystem(producerSrSystem);
        systemTracker.addSystem(orchestratorSrSystem);

        ruleStore = new InMemoryRuleStore();
        orchestratorClient = new OrchestratorClient(httpClient, cloud, ruleStore, systemTracker, pdTracker);
    }

    @Test
    public void shouldCreateInsecureRule() throws PdStoreException {
        final PlantDescriptionEntryDto entry = createEntry();
        pdTracker.put(entry);
        var rule = orchestratorClient.createRule(entry.connections().get(0));
        assertEquals(cloud.name(), rule.cloud().name());
        assertEquals(cloud.operator(), rule.cloud().operator());
        assertEquals(consumerSrSystem.id(), rule.consumerSystemId());
        assertEquals(producerSrSystem.systemName(), rule.providerSystem().systemName());
        assertEquals(producerSystem.ports().get(0).serviceDefinition(), rule.serviceDefinitionName());
        assertEquals(cloud.name(), rule.cloud().name());
        assertEquals(cloud.operator(), rule.cloud().operator());
        assertEquals("HTTP-INSECURE-JSON", rule.serviceInterfaceName());
    }

    @Test
    public void shouldCreateSecureRule() throws PdStoreException {
        final PlantDescriptionEntryDto entry = createEntry();
        final var connection = entry.connections().get(0);

        pdTracker.put(entry);

        when(httpClient.isSecure()).thenReturn(true);

        var rule = orchestratorClient.createRule(connection);
        assertEquals(cloud.name(), rule.cloud().name());
        assertEquals(cloud.operator(), rule.cloud().operator());
        assertEquals(consumerSrSystem.id(), rule.consumerSystemId());
        assertEquals(producerSrSystem.systemName(), rule.providerSystem().systemName());
        assertEquals(producerSystem.ports().get(0).serviceDefinition(), rule.serviceDefinitionName());
        assertEquals(cloud.name(), rule.cloud().name());
        assertEquals(cloud.operator(), rule.cloud().operator());
        assertEquals("HTTP-SECURE-JSON", rule.serviceInterfaceName());
    }

    /**
     * Two plant descriptions are created, one including the other. The active
     * plant description contains a connection between its own system and a
     * system in the included Plant Description.
     */
    @Test
    public void shouldAllowConnectionsToSystemInIncludedEntry() throws PdStoreException {
        int entryIdB = 442;
        String producerIdB = "system_3";
        String producerNameB = "System 3";
        String producerPortB = "port_3";

        final var entryA = createEntry();

        final List<PortDto> producerPortsB = List.of(
            new PortBuilder()
                .portName(producerPortB)
                .serviceDefinition(serviceDefinitionA)
                .consumer(false)
                .build());

        final PdeSystemDto producerSystemB = new PdeSystemBuilder()
            .systemId(producerIdB)
            .systemName(producerNameB)
            .ports(producerPortsB)
            .build();

        final ConnectionDto connection = new ConnectionBuilder()
            .consumer(new SystemPortBuilder()
                .systemId(consumerId)
                .portName(consumerPort)
                .build())
            .producer(new SystemPortBuilder()
                .systemId(producerIdB)
                .portName(producerPortB)
                .build())
            .build();

        final var entryB = new PlantDescriptionEntryBuilder()
            .id(entryIdB)
            .plantDescription("Plant Description B")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .include(List.of(entryA.id()))
            .systems(List.of(producerSystemB))
            .connections(List.of(connection))
            .build();

        final SrSystemDto producerSrSystemB = new SrSystemBuilder()
            .id(97)
            .systemName(producerNameB)
            .address("0.0.0.12")
            .port(5016)
            .authenticationInfo(null)
            .createdAt(now.toString())
            .updatedAt(now.toString())
            .build();

        systemTracker.addSystem(producerSrSystemB);

        pdTracker.put(entryA);
        pdTracker.put(entryB);

        var rule = orchestratorClient.createRule(connection);
        assertEquals(cloud.name(), rule.cloud().name());
        assertEquals(cloud.operator(), rule.cloud().operator());
        assertEquals(consumerSrSystem.id(), rule.consumerSystemId());
        assertEquals(producerNameB, rule.providerSystem().systemName());
        assertEquals(producerSystemB.ports().get(0).serviceDefinition(), rule.serviceDefinitionName());
        assertEquals(cloud.name(), rule.cloud().name());
        assertEquals(cloud.operator(), rule.cloud().operator());
        assertEquals("HTTP-INSECURE-JSON", rule.serviceInterfaceName());
    }

    @Test
    public void shouldNotCreateRuleWithMissingConsumer() throws PdStoreException {
        systemTracker.remove(consumerName);
        final var entry = createEntry();
        final var connection = entry.connections().get(0);
        pdTracker.put(entry);
        assertNull(orchestratorClient.createRule(connection));
    }

    @Test
    public void shouldNotCreateRuleWithMissingProvider() throws PdStoreException {
        systemTracker.remove(producerName);
        final var entry = createEntry();
        final var connection = entry.connections().get(0);
        pdTracker.put(entry);
        assertNull(orchestratorClient.createRule(connection));
    }

    @Test
    public void shouldStoreRulesWhenAddingPd() throws RuleStoreException, PdStoreException {

        final PlantDescriptionEntryDto entry = createEntry();
        pdTracker.put(entry);

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse response = new MockClientResponse();
        int ruleId = 39;
        response
            .status(HttpStatus.CREATED)
            .body(new StoreEntryListBuilder()
                .count(1)
                .data(List.of(createStoreEntryRule(ruleId, producerSrSystem, consumerSrSystem)))
                .build());

        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
            .thenReturn(Future.success(response));

        orchestratorClient.onPlantDescriptionAdded(entry);

        // Verify that the rule was stored correctly:
        assertTrue(ruleStore.readRules().contains(ruleId));

        // Verify that the HTTP client was passed correct data:
        ArgumentCaptor<InetSocketAddress> addressCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
        ArgumentCaptor<HttpClientRequest> requestCaptor = ArgumentCaptor.forClass(HttpClientRequest.class);

        verify(httpClient).send(addressCaptor.capture(), requestCaptor.capture());

        InetSocketAddress capturedAddress = addressCaptor.getValue();
        HttpClientRequest capturedRequest = requestCaptor.getValue();

        assertEquals(HttpMethod.POST, capturedRequest.method().orElse(null));
        assertEquals("/orchestrator/mgmt/store", capturedRequest.uri().orElse(null));
        assertEquals(orchestratorSrSystem.address(), capturedAddress.getAddress().getHostAddress());
        assertEquals(orchestratorSrSystem.port(), capturedAddress.getPort());

        assertTrue(capturedRequest.body().isPresent());
        @SuppressWarnings("unchecked") List<StoreRule> rulesSent = (List<StoreRule>) capturedRequest.body().get();
        assertEquals(1, rulesSent.size());
        StoreRule ruleSent = rulesSent.get(0);
        assertEquals(1, ruleSent.priority());
        assertEquals(serviceDefinitionA, ruleSent.serviceDefinitionName());
        assertEquals(producerSrSystem.systemName(), ruleSent.providerSystem().systemName());
        assertEquals(producerSrSystem.address(), ruleSent.providerSystem().address());
        assertEquals(producerSrSystem.port(), ruleSent.providerSystem().port());
        assertEquals(consumerSrSystem.id(), ruleSent.consumerSystemId());
        assertEquals(cloud.name(), ruleSent.cloud().name());
        assertEquals(cloud.operator(), ruleSent.cloud().operator());
    }

    @Test
    public void shouldNotCreateRulesWhenSystemIsMissing() throws RuleStoreException, PdStoreException {

        final PlantDescriptionEntryDto entry = createEntry();
        pdTracker.put(entry);

        systemTracker.remove(consumerName); // A system is now missing
        orchestratorClient.onPlantDescriptionAdded(entry);

        assertTrue(ruleStore.readRules().isEmpty());

    }

    @Test
    public void shouldNotCreateRulesForPdWithoutConnections() throws
        PdStoreException {

        final PlantDescriptionEntryDto entry = new PlantDescriptionEntryBuilder()
            .id(0).plantDescription("Plant Description 1A")
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .systems(List.of(consumerSystem, producerSystem))
            // No connections
            .build();

        pdTracker.put(entry);

        orchestratorClient.initialize()
            .ifSuccess(result -> {
                verify(httpClient, never()).send(any(), any());
                assertTrue(ruleStore.readRules().isEmpty());
            }).onFailure(e -> {
            fail(); // We should never get here.
        });
    }

    @Test
    public void shouldRemoveRulesWhenRemovingActiveEntry() throws RuleStoreException, PdStoreException {
        final PlantDescriptionEntryDto activeEntry = createEntry();
        pdTracker.put(activeEntry);

        int ruleId = 65;

        ruleStore.setRules(Set.of(ruleId));

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse deletionResponse = new MockClientResponse();
        deletionResponse.status(HttpStatus.OK);

        final MockClientResponse creationResponse = new MockClientResponse();
        int newRuleId = 82;
        creationResponse.status(HttpStatus.CREATED);
        creationResponse.body(createSingleRuleStoreList(newRuleId, producerSrSystem, consumerSrSystem));

        //noinspection unchecked
        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
            .thenReturn(
                Future.success(deletionResponse),
                Future.success(creationResponse),
                Future.success(deletionResponse)
            );

        orchestratorClient.initialize()
            .ifSuccess(result -> {
                assertEquals(1, ruleStore.readRules().size());
                orchestratorClient.onPlantDescriptionRemoved(activeEntry);

                // Verify that the HTTP client was passed correct data:
                ArgumentCaptor<InetSocketAddress> addressCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
                ArgumentCaptor<HttpClientRequest> requestCaptor = ArgumentCaptor.forClass(HttpClientRequest.class);

                verify(httpClient, times(3)).send(addressCaptor.capture(), requestCaptor.capture());

                InetSocketAddress capturedAddress = addressCaptor.getValue();
                HttpClientRequest capturedRequest = requestCaptor.getValue();

                // Assert that the Orchestrator was called with the proper data.
                assertEquals("/" + orchestratorSrSystem.address(), capturedAddress.getAddress().toString());
                assertEquals(orchestratorSrSystem.port(), capturedAddress.getPort());
                assertEquals("/orchestrator/mgmt/store/" + newRuleId, capturedRequest.uri().orElse(null));
                assertTrue(ruleStore.readRules().isEmpty());
            }).onFailure(e -> {
            fail(); // We should never get here.
        });
    }

    @Test
    public void shouldHandleStoreRemovalFailure() throws RuleStoreException, PdStoreException {
        final PlantDescriptionEntryDto activeEntry = createEntry();

        // Use a mock rule store in order to make it throw exceptions.
        ruleStore = Mockito.mock(InMemoryRuleStore.class);

        String errorMessage = "Mocked exception";
        doThrow(new RuleStoreException(errorMessage)).when(ruleStore).readRules();

        // We need to reinstantiate this with the mock rule store.
        orchestratorClient = new OrchestratorClient(httpClient, cloud, ruleStore, systemTracker, pdTracker);

        pdTracker.put(activeEntry);
        ruleStore.setRules(Set.of(12));

        orchestratorClient.initialize()
            .ifSuccess(result -> {
                fail(); // We should never get here.
            }).onFailure(e -> assertEquals(
            errorMessage,
            e.getMessage()
        ));
    }

    @Test
    public void shouldRemoveRulesWhenRemovingConnections() throws RuleStoreException, PdStoreException {
        final PlantDescriptionEntryDto entry = createEntry();

        pdTracker.put(entry);
        int ruleId = 65;
        ruleStore.setRules(Set.of(ruleId));

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse deletionResponse = new MockClientResponse();
        deletionResponse.status(HttpStatus.OK);

        final MockClientResponse creationResponse = new MockClientResponse();
        int newRuleId = 82;
        creationResponse.status(HttpStatus.CREATED);
        creationResponse.body(createSingleRuleStoreList(newRuleId, producerSrSystem, consumerSrSystem));

        //noinspection unchecked
        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
            .thenReturn(
                Future.success(deletionResponse),
                Future.success(creationResponse),
                Future.success(deletionResponse)
            );

        orchestratorClient.initialize()
            .ifSuccess(result -> {
                assertEquals(1, ruleStore.readRules().size());

                final PlantDescriptionEntryDto entryWithoutConnections = new PlantDescriptionEntryBuilder()
                    .id(entry.id())
                    .plantDescription(entry.plantDescription())
                    .createdAt(entry.createdAt())
                    .updatedAt(now)
                    .active(true)
                    .build();
                pdTracker.put(entryWithoutConnections);
                orchestratorClient.onPlantDescriptionUpdated(entryWithoutConnections);

                // Verify that the HTTP client was passed correct data:
                ArgumentCaptor<InetSocketAddress> addressCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
                ArgumentCaptor<HttpClientRequest> requestCaptor = ArgumentCaptor.forClass(HttpClientRequest.class);

                verify(httpClient, times(3)).send(addressCaptor.capture(), requestCaptor.capture());

                InetSocketAddress capturedAddress = addressCaptor.getValue();
                HttpClientRequest capturedRequest = requestCaptor.getValue();

                // Assert that the Orchestrator was called with the proper data.
                assertEquals("/" + orchestratorSrSystem.address(), capturedAddress.getAddress().toString());
                assertEquals(orchestratorSrSystem.port(), capturedAddress.getPort());
                assertEquals("/orchestrator/mgmt/store/" + newRuleId, capturedRequest.uri().orElse(null));
                assertTrue(ruleStore.readRules().isEmpty());
            }).onFailure(e -> {
            fail(); // We should never get here.
        });
    }

    @Test
    public void shouldNotChangeRulesWhenRemovingInactiveEntry() throws RuleStoreException, PdStoreException {

        final PlantDescriptionEntryDto entryA = createEntry();
        final PlantDescriptionEntryDto entryB = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description B")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .build();

        pdTracker.put(entryA);
        pdTracker.put(entryB);

        int ruleId = 65;

        final RuleStore ruleStore = new InMemoryRuleStore();
        ruleStore.setRules(Set.of(ruleId));

        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, ruleStore, systemTracker, pdTracker);

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse deletionResponse = new MockClientResponse();
        deletionResponse.status(HttpStatus.OK);

        final MockClientResponse creationResponse = new MockClientResponse();
        int newRuleId = 2;
        creationResponse.status(HttpStatus.CREATED);
        creationResponse.body(createSingleRuleStoreList(newRuleId, producerSrSystem, consumerSrSystem));

        //noinspection unchecked
        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
            .thenReturn(
                Future.success(deletionResponse),
                Future.success(creationResponse)
            );

        orchestratorClient.initialize()
            .ifSuccess(result -> {
                orchestratorClient.onPlantDescriptionRemoved(entryB);
                assertEquals(1, ruleStore.readRules().size());
                assertTrue(ruleStore.readRules().contains(newRuleId));
            }).onFailure(e -> {
            fail(); // We should never get here.
        });
    }

    @Test
    public void shouldHandleRemovalOfInactivePd() throws PdStoreException {

        final PlantDescriptionEntryDto inactiveEntry = PlantDescriptionEntry.deactivated(createEntry());
        pdTracker.put(inactiveEntry);

        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, ruleStore, systemTracker, pdTracker);
        orchestratorClient.initialize()
            .ifSuccess(result -> {
                assertTrue(ruleStore.readRules().isEmpty());
                orchestratorClient.onPlantDescriptionRemoved(inactiveEntry);
                assertTrue(ruleStore.readRules().isEmpty());
            }).onFailure(e -> {
            fail(); // We should never get here.
        });
    }

    @Test
    public void shouldRemoveRulesWhenSettingInactive() throws RuleStoreException, PdStoreException {
        final PlantDescriptionEntryDto activeEntry = createEntry();

        pdTracker.put(activeEntry);
        int ruleId = 512;
        ruleStore.setRules(Set.of(ruleId));

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse deletionResponse = new MockClientResponse();
        deletionResponse.status(HttpStatus.OK);

        final MockClientResponse creationResponse = new MockClientResponse();
        int newRuleId = 2;
        creationResponse.status(HttpStatus.CREATED);
        creationResponse.body(createSingleRuleStoreList(newRuleId, producerSrSystem, consumerSrSystem));

        //noinspection unchecked
        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
            .thenReturn(
                Future.success(deletionResponse),
                Future.success(creationResponse),
                Future.success(deletionResponse)
            );

        orchestratorClient.initialize()
            .ifSuccess(result -> {
                var deactivatedEntry = PlantDescriptionEntry.deactivated(activeEntry);
                orchestratorClient.onPlantDescriptionUpdated(deactivatedEntry);
                assertEquals(0, ruleStore.readRules().size());
            }).onFailure(e -> {
            fail(); // We should never get here.
        });
    }

    @Test
    public void shouldNotTouchRulesWhenUpdatingInactiveToInactive() throws PdStoreException {

        final PlantDescriptionEntryDto entryA = new PlantDescriptionEntryBuilder()
            .id(0)
            .plantDescription("Plant Description A")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .build();
        final PlantDescriptionEntryDto entryB = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description B")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .build();

        pdTracker.put(entryA);
        pdTracker.put(entryB);

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse deletionResponse = new MockClientResponse();
        deletionResponse.status(HttpStatus.OK);

        orchestratorClient.initialize()
            .ifSuccess(result -> {
                orchestratorClient.onPlantDescriptionUpdated(entryB);
                verify(httpClient, never()).send(any(), any());
                assertTrue(ruleStore.readRules().isEmpty());
            }).onFailure(e -> {
            fail(); // We should never get here.
        });
    }

    @Test
    public void rulesShouldBeEmptyAfterFailedPost() throws RuleStoreException, PdStoreException {

        final PlantDescriptionEntryDto entry = createEntry();

        pdTracker.put(entry);
        int ruleId = 25;

        final RuleStore ruleStore = new InMemoryRuleStore();
        ruleStore.setRules(Set.of(ruleId));

        final var orchestratorClient = new OrchestratorClient(httpClient, cloud, ruleStore, systemTracker, pdTracker);

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse deletionResponse = new MockClientResponse();
        deletionResponse.status(HttpStatus.OK);

        final MockClientResponse failedCreationResponse = new MockClientResponse();
        failedCreationResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);

        final MockClientResponse creationResponse = new MockClientResponse();
        int newRuleId = 23;
        creationResponse.status(HttpStatus.CREATED);
        creationResponse.body(createSingleRuleStoreList(newRuleId, producerSrSystem, consumerSrSystem));

        //noinspection unchecked
        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
            .thenReturn(
                Future.success(deletionResponse),
                Future.success(creationResponse),
                Future.success(deletionResponse),
                // An error occurs when POSTing new rules:
                Future.failure(new RuntimeException("Some error"))
            );

        orchestratorClient.initialize()
            .ifSuccess(result -> {
                orchestratorClient.onPlantDescriptionUpdated(entry);
                assertTrue(ruleStore.readRules().isEmpty());
            }).onFailure(e -> {
            fail(); // We should never get here.
        });
    }

    @Test
    public void shouldHandleFailedDeleteRequest() throws RuleStoreException, PdStoreException {

        final PlantDescriptionEntryDto activeEntry = createEntry();

        pdTracker.put(activeEntry);
        ruleStore.setRules(Set.of(65));

        // Create some fake data for the HttpClient to respond with:
        final MockClientResponse deletionResponse = new MockClientResponse();
        deletionResponse.status(HttpStatus.OK);

        final MockClientResponse creationResponse = new MockClientResponse();
        int newRuleId = 82;

        creationResponse
            .status(HttpStatus.CREATED)
            .body(createSingleRuleStoreList(newRuleId, producerSrSystem, consumerSrSystem));

        final MockClientResponse failedDeletionResponse = new MockClientResponse();
        failedDeletionResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);

        //noinspection unchecked
        when(httpClient.send(any(InetSocketAddress.class), any(HttpClientRequest.class)))
            .thenReturn(
                Future.success(deletionResponse),
                Future.success(creationResponse),
                Future.success(failedDeletionResponse)
            );

        orchestratorClient.initialize()
            .ifSuccess(result -> {
                orchestratorClient.onPlantDescriptionRemoved(activeEntry);
                // The rule should not have been removed.
                assertTrue(ruleStore.readRules().contains(newRuleId));
            }).onFailure(e -> {
            fail(); // We should never get here.
        });
    }
}