package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.ConnectionBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.ConnectionDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescription;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.SystemPortBuilder;
import eu.arrowhead.core.plantdescriptionengine.utils.MockPdStore;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockServiceResponse;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceResponse;

public class AddPlantDescriptionTest {

    @Test
    public void shouldCreateEntry() throws PdStoreException {

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new AddPlantDescription(pdTracker);
        final PlantDescription description = TestUtils.createDescription();
        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.CREATED, response.status().get());
                    assertNotNull(response.body());

                    PlantDescriptionEntry entry = (PlantDescriptionEntry)response.body().get();
                    assertTrue(entry.matchesDescription(description));

                    var entryInMap = pdTracker.get(entry.id());
                    assertNotNull(entryInMap);
                    // TODO: Compare 'entryInMap' with 'entry'.
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectMissingConsumer() throws PdStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String missingId = "garbage_string";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String serviceDefinition = "service_a";

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new AddPlantDescription(pdTracker);

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
        final var description = new PlantDescriptionBuilder()
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .build();

        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<A connection refers to the missing system '" + missingId + "'>";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectMissingProvider() throws PdStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String missingId = "garbage_string";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String serviceDefinition = "service_a";

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new AddPlantDescription(pdTracker);

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
        final var description = new PlantDescriptionBuilder()
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .build();

        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<A connection refers to the missing system '" + missingId + "'>";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectInvalidConsumerPort() throws PdStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String invalidPort = "no_such_port";
        final String serviceDefinition = "service_a";

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new AddPlantDescription(pdTracker);

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
        final var description = new PlantDescriptionBuilder()
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .build();

        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<Connection refers to the missing consumer port '" +
                        invalidPort + "'>";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectInvalidProducerPort() throws PdStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String invalidPort = "no_such_port";
        final String serviceDefinition = "service_a";

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new AddPlantDescription(pdTracker);

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
        final var description = new PlantDescriptionBuilder()
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .build();

        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<Connection refers to the missing producer port '" +
                        invalidPort + "'>";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRequireMetadataToDifferentiateBetweenPorts() throws PdStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPortA = "consumerPortA";
        final String producerPort = "port_2";
        final String serviceDefinition = "service_a";

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new AddPlantDescription(pdTracker);

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
        final var description = new PlantDescriptionBuilder()
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .systems(List.of(consumerSystem, producerSystem))
            .connections(connections)
            .build();

        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<" + consumerId + " has multiple ports with service definition '" +
                        serviceDefinition + "' without metadata.>";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectNonuniqueMetadata() throws PdStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPortA = "consumerPortA";
        final String producerPort = "port_2";
        final String serviceDefinition = "service_a";
        final Map<String, String> sharedMetadata = Map.of("x", "y");

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new AddPlantDescription(pdTracker);

        final List<PortDto> consumerPorts = List.of(
            new PortBuilder()
                .portName(consumerPortA)
                .serviceDefinition(serviceDefinition)
                .metadata(sharedMetadata)
                .consumer(true)
                .build(),
            new PortBuilder()
                .portName("port_b")
                .serviceDefinition(serviceDefinition)
                .metadata(sharedMetadata)
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

        final var description = new PlantDescriptionBuilder()
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .connections(new ArrayList<>())
            .systems(List.of(consumerSystem, producerSystem))
            .build();

        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<" + consumerId +
                        " has duplicate metadata for ports with service definition '" + serviceDefinition + "'>";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldAcceptUniqueMetadata() throws PdStoreException {

        final String serviceDefinition = "service_a";
        final Map<String, String> metadataA = Map.of("a", "1");
        final Map<String, String> metadataB = Map.of("a", "2");

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new AddPlantDescription(pdTracker);

        final List<PortDto> consumerPorts = List.of(
            new PortBuilder()
                .portName("port_a")
                .serviceDefinition(serviceDefinition)
                .metadata(metadataA)
                .consumer(true)
                .build(),
            new PortBuilder()
                .portName("port_b")
                .serviceDefinition(serviceDefinition)
                .metadata(metadataB)
                .consumer(true)
                .build()
        );

        final PdeSystemDto consumerSystem = new PdeSystemBuilder()
            .systemId("system_a")
            .ports(consumerPorts)
            .build();

        final var description = new PlantDescriptionBuilder()
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .connections(new ArrayList<>())
            .systems(List.of(consumerSystem))
            .build();

        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.CREATED, response.status().get());
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRequireUniquePortnames() throws PdStoreException {
        final String systemId = "system_a";
        final String portName = "port_a";

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new AddPlantDescription(pdTracker);

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

        final var description = new PlantDescriptionBuilder()
            .plantDescription("Plant Description 1A")
            .active(true)
            .systems(List.of(consumerSystem))
            .include(new ArrayList<>())
            .connections(new ArrayList<>())
            .build();

        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<Duplicate port name '" +
                        portName + "' in system '" + systemId + "'>";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldHandleBackingStoreFailure() throws PdStoreException {

        final var backingStore = new MockPdStore();
        final var pdTracker = new PlantDescriptionTracker(backingStore);
        final var handler = new AddPlantDescription(pdTracker);

        final PlantDescription description = TestUtils.createDescription();
        final HttpServiceResponse response = new MockServiceResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        backingStore.setFailOnNextWrite();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status().get());
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }

    }

}