package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.InMemoryBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionUpdate;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionUpdateBuilder;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class UpdatePlantDescriptionTest {

    @Test
    public void shouldReplaceExistingEntries() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new UpdatePlantDescription(entryMap);
        final int entryId = 87;

        final PlantDescriptionEntryDto entry = TestUtils.createEntry(entryId);
        final String newName = entry.plantDescription() + " modified";
        final PlantDescriptionUpdate update = new PlantDescriptionUpdateBuilder()
            .plantDescription(newName)
            .build();
        final HttpServiceResponse response = new MockResponse();
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
            .body(update)
            .build();

        entryMap.put(entry);

        final int sizeBeforePut = entryMap.getEntries().size();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.OK, response.status().get());
                    PlantDescriptionEntry returnedEntry = (PlantDescriptionEntry)response.body().get();
                    assertEquals(returnedEntry.plantDescription(), newName);
                    assertEquals(sizeBeforePut, entryMap.getEntries().size());
                })
                .onFailure(throwable -> {
                    assertNull(throwable);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectInvalidId() throws BackingStoreException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new UpdatePlantDescription(entryMap);
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
        final var handler = new UpdatePlantDescription(entryMap);
        final int nonExistentId = 9;

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