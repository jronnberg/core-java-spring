package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.Utils;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.InMemoryBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class GetPlantDescriptionTest {

    @Test
    public void shouldRespondWithNotFound() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new GetPlantDescription(entryMap);
        final int nonExistentEntryId = 0;

        HttpServiceRequest request = new MockRequest.Builder()
                .pathParameters(List.of(String.valueOf(nonExistentEntryId))).build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response).ifSuccess(result -> {
                String expectedBody = "Plant Description with ID " + nonExistentEntryId + " not found.";
                assertEquals(HttpStatus.NOT_FOUND, response.status().get());
                assertEquals(response.body().get(), expectedBody);
            }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRespondWithStoredEntry() throws BackingStoreException {

        int entryId = 39;

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        entryMap.put(Utils.createEntry(entryId));

        GetPlantDescription handler = new GetPlantDescription(entryMap);

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
            .build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.OK, response.status().get());

                assertTrue(response.body().isPresent());
                var returnedEntry = (PlantDescriptionEntry)response.body().get();
                assertEquals(returnedEntry.id(), entryId); // TODO: Add 'equals' method to entries and do a proper comparison?
            }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldNotAcceptInvalidId() throws BackingStoreException {

        int entryId = 24;
        String invalidId = "Invalid";

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        entryMap.put(Utils.createEntry(entryId));

        GetPlantDescription handler = new GetPlantDescription(entryMap);

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(invalidId))
            .build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertTrue(response.status().isPresent());
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

}