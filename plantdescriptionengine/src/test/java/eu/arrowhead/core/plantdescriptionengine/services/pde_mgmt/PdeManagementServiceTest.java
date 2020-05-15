package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.List;

import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.BackingStore.NullBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class PdeManagementServiceTest {

    @Test
    public void shouldRespondWithNotFound() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        OnDescriptionsGet handler = new OnDescriptionsGet(entryMap);

        HttpServiceRequest request = new MockRequest();
        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(response.status().isPresent());
        assertEquals(response.status().get(), HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldRespondWithStoredEntry() throws BackingStoreException {

        int entryId = 39;

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        entryMap.put(Utils.createEntry(entryId));

        OnDescriptionsGet handler = new OnDescriptionsGet(entryMap);

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
            .build();

        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(response.status().isPresent());
        assertEquals(response.status().get(), HttpStatus.OK);

        assertTrue(response.body().isPresent());
        var returnedEntry = (PlantDescriptionEntry)response.body().get();
        assertEquals(returnedEntry.id(), entryId);
        // TODO: Add 'equals' method to entries and do a proper comparison?

    }

    @Test
    public void shouldNotAcceptInvalidId() throws BackingStoreException {

        int entryId = 24;
        String invalidId = "Invalid";

        final var entryMap = new PlantDescriptionEntryMap(new NullBackingStore());
        entryMap.put(Utils.createEntry(entryId));

        OnDescriptionsGet handler = new OnDescriptionsGet(entryMap);

        HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(invalidId))
            .build();

        HttpServiceResponse response = new MockResponse();


        try {
            handler.handle(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(response.status().isPresent());
        assertEquals(response.status().get(), HttpStatus.BAD_REQUEST);
    }

}