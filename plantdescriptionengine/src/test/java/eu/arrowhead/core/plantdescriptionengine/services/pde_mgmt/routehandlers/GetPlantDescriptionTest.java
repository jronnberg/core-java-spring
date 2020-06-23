package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class GetPlantDescriptionTest {

    @Test
    public void shouldRespondWithNotFound() throws PdStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryPdStore());
        final var handler = new GetPlantDescription(entryMap);
        final int nonExistentEntryId = 0;

        HttpServiceRequest request = new MockRequest.Builder()
                .pathParameters(List.of(String.valueOf(nonExistentEntryId))).build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response).ifSuccess(result -> {
                assertEquals(HttpStatus.NOT_FOUND, response.status().get());
                String expectedErrorMessage = "Plant Description with ID " + nonExistentEntryId + " not found.";
                String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                assertEquals(expectedErrorMessage, actualErrorMessage);
            }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void shouldRespondWithStoredEntry() throws PdStoreException {

        int entryId = 39;

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryPdStore());
        entryMap.put(TestUtils.createEntry(entryId));

        GetPlantDescription handler = new GetPlantDescription(entryMap);

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
            .build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertEquals(HttpStatus.OK, response.status().get());

                var returnedEntry = (PlantDescriptionEntry)response.body().get();
                assertEquals(returnedEntry.id(), entryId, 0); // TODO: Add 'equals' method to entries and do a proper comparison?
            }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldNotAcceptInvalidId() throws PdStoreException {

        int entryId = 24;
        String invalidId = "Invalid";

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryPdStore());
        entryMap.put(TestUtils.createEntry(entryId));

        GetPlantDescription handler = new GetPlantDescription(entryMap);

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(invalidId))
            .build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
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