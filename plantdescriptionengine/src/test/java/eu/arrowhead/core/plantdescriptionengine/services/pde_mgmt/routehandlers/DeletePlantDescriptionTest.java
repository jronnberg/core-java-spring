package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.pdentrymap.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.InMemoryBackingStore;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class DeletePlantDescriptionTest {

    @Test
    public void shouldDeleteEntries() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new DeletePlantDescription(entryMap);
        final int entryId = 14;
        entryMap.put(TestUtils.createEntry(entryId));

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
            .build();

        HttpServiceResponse response = new MockResponse();

        // Make sure that the entry is there before we delete it.
        assertNotNull(entryMap.get(entryId));

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertTrue(response.status().isPresent());
                    assertEquals(HttpStatus.OK, response.status().get());
                    assertNull(entryMap.get(entryId));
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectInvalidId() throws BackingStoreException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new DeletePlantDescription(entryMap);
        final String invalidEntryId = "InvalidId";

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(invalidEntryId))
            .build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertTrue(response.status().isPresent());
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    assertTrue(response.body().isPresent());

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
    public void shouldRejectNonexistentIds() throws BackingStoreException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new DeletePlantDescription(entryMap);
        final int nonExistentId = 392;

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(nonExistentId)))
            .build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.status().get());
                    assertTrue(response.body().isPresent());
                    assertEquals(
                        response.body().get(),
                        "Plant Description with ID " + nonExistentId + " not found."
                    );
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }
}