package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.utils.MockPdStore;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockServiceResponse;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class DeletePlantDescriptionTest {

    @Test
    public void shouldDeleteEntries() throws PdStoreException {

        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new DeletePlantDescription(pdTracker);
        final int entryId = 14;
        pdTracker.put(TestUtils.createEntry(entryId));

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
            .build();

        HttpServiceResponse response = new MockServiceResponse();

        // Make sure that the entry is there before we delete it.
        assertNotNull(pdTracker.get(entryId));

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.OK, response.status().get());
                    assertNull(pdTracker.get(entryId));
                })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectInvalidId() throws PdStoreException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new DeletePlantDescription(pdTracker);
        final String invalidEntryId = "InvalidId";

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(invalidEntryId))
            .build();

        HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "'" + invalidEntryId + "' is not a valid Plant Description Entry ID.";
                    String actualErrorMessage = ((ErrorMessage) response.body().get()).error();
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
    public void shouldRejectNonexistentIds() throws PdStoreException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new DeletePlantDescription(pdTracker);
        final int nonExistentId = 392;

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(nonExistentId)))
            .build();

        HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.status().get());
                    String expectedErrorMessage = "Plant Description with ID " + nonExistentId + " not found.";
                    String actualErrorMessage = ((ErrorMessage) response.body().get()).error();
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
    public void shouldRejectDeletionOfIncludedEntry() throws PdStoreException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var handler = new DeletePlantDescription(pdTracker);
        final Instant now = Instant.now();
        final int entryIdA = 23;
        final int entryIdB = 24;

        final var entryA = new PlantDescriptionEntryBuilder()
            .id(entryIdA)
            .plantDescription("A")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .build();
        final var entryB = new PlantDescriptionEntryBuilder()
            .id(entryIdB)
            .plantDescription("B")
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .include(List.of(entryIdA))
            .build();

        pdTracker.put(entryA);
        pdTracker.put(entryB);

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryIdA)))
            .build();

        HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<Error in include list: Entry '" + entryIdA
                        + "' is required by entry '" + entryIdB + "'.>";
                    String actualErrorMessage = ((ErrorMessage) response.body().get()).error();
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
        final var handler = new DeletePlantDescription(pdTracker);
        final int entryId = 87;
        pdTracker.put(TestUtils.createEntry(entryId));

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
            .build();

        HttpServiceResponse response = new MockServiceResponse();

        backingStore.setFailOnNextRemove();

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