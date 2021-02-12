package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescription;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PortDto;
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
    public void shouldReportInvalidDescription() throws PdStoreException {
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
                .portName(portName) // Duplicate port name, should be reported!
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