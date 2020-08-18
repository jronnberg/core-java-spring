package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PortBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PortDto;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class ReplacePlantDescriptionTest {

    @Test
    public void shouldCreateEntry() throws PdStoreException {

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new ReplacePlantDescription(pdTracker);
        final int entryId = 87;
        final PlantDescription description = TestUtils.createDescription();
        final HttpServiceResponse response = new MockResponse();
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
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
    public void shouldReplaceExistingEntries() throws PdStoreException {

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new ReplacePlantDescription(pdTracker);
        final int entryId = 87;

        final PlantDescriptionEntryDto entry = TestUtils.createEntry(entryId);
        final String newName = entry.plantDescription() + " modified";
        final PlantDescription description = new PlantDescriptionBuilder()
            .plantDescription(newName)
            .active(true)
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .build();
        final HttpServiceResponse response = new MockResponse();
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
            .body(description)
            .build();

        pdTracker.put(entry);

        final int sizeBeforePut = pdTracker.getEntries().size();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.CREATED, response.status().get());
                    assertNotNull(response.body());
                    PlantDescriptionEntry returnedEntry = (PlantDescriptionEntry)response.body().get();
                    assertEquals(returnedEntry.plantDescription(), newName);
                    assertEquals(sizeBeforePut, pdTracker.getEntries().size());
                })
                .onFailure(throwable -> {
                    assertNull(throwable);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectInvalidId() throws PdStoreException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new ReplacePlantDescription(pdTracker);
        final String invalidEntryId = "InvalidId";

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(invalidEntryId))
            .build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());

                    String expectedBody = invalidEntryId + " is not a valid Plant Description Entry ID.";
                    assertEquals(response.body().get(), expectedBody);
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
        final int entryId = 1;
        final String systemId = "system_a";
        final String portName = "port_a";

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new ReplacePlantDescription(pdTracker);

        pdTracker.put(TestUtils.createEntry(entryId));

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

        final HttpServiceResponse response = new MockResponse();
        final MockRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
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
}