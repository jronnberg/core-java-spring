package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.NullBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandler.DescriptionDeleteHandler;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class DescriptionDeleteHandlerTest {

    @Test
    public void shouldDeleteEntries() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        final var handler = new DescriptionDeleteHandler(entryMap);
        final int entryId = 14;
        entryMap.put(Utils.createEntry(entryId));

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
                .onFailure(throwable -> {
                    throwable.printStackTrace();
                    assertNull(throwable);
                });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void shouldHandleRejectInvalidId() throws BackingStoreException {
        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        final var handler = new DescriptionDeleteHandler(entryMap);
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

                    String expectedBody = invalidEntryId + " is not a valid plant description entry ID.";
                    assertEquals(response.body().get(), expectedBody);
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }
}