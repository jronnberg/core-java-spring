package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.InMemoryBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.ConnectionBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.ConnectionDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PortBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PortDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.SystemPortBuilder;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceResponse;

public class AddPlantDescriptionTest {

    @Test
    public void shouldCreateEntry() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new AddPlantDescription(entryMap);
        final PlantDescription description = TestUtils.createDescription();
        final HttpServiceResponse response = new MockResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertTrue(response.status().isPresent());
                    assertEquals(HttpStatus.CREATED, response.status().get());
                    assertNotNull(response.body());
                    assertTrue(response.body().isPresent());

                    PlantDescriptionEntry entry = (PlantDescriptionEntry)response.body().get();
                    assertTrue(entry.matchesDescription(description));

                    var entryInMap = entryMap.get(entry.id());
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
    public void shouldRejectMissingConsumer() throws BackingStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String missingId = "garbage_string";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String serviceDefinition = "service_a";

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new AddPlantDescription(entryMap);

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

        final HttpServiceResponse response = new MockResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertTrue(response.status().isPresent());
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    assertTrue(response.body().isPresent());
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
    public void shouldRejectMissingProvider() throws BackingStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String missingId = "garbage_string";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String serviceDefinition = "service_a";

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new AddPlantDescription(entryMap);

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

        final HttpServiceResponse response = new MockResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertTrue(response.status().isPresent());
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    assertTrue(response.body().isPresent());
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
    public void shouldRejectInvalidConsumerPort() throws BackingStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String invalidPort = "no_such_port";
        final String serviceDefinition = "service_a";

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new AddPlantDescription(entryMap);

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

        final HttpServiceResponse response = new MockResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertTrue(response.status().isPresent());
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    assertTrue(response.body().isPresent());
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
    public void shouldRejectInvalidProducerPort() throws BackingStoreException {

        final String consumerId = "system_1";
        final String producerId = "system_2";
        final String consumerPort = "port_1";
        final String producerPort = "port_2";
        final String invalidPort = "no_such_port";
        final String serviceDefinition = "service_a";

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new AddPlantDescription(entryMap);

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

        final HttpServiceResponse response = new MockResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertTrue(response.status().isPresent());
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    assertTrue(response.body().isPresent());
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

}