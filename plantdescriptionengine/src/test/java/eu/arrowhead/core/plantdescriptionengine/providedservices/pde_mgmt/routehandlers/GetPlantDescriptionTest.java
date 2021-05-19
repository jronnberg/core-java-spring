package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockServiceResponse;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class GetPlantDescriptionTest {

    @Test
    public void shouldRespondWithNotFound() throws PdStoreException {

        final PlantDescriptionTracker pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final GetPlantDescription handler = new GetPlantDescription(pdTracker);
        final int nonExistentEntryId = 0;

        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(nonExistentEntryId)))
            .build();

        final MockServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.status().orElse(null));
                    final String expectedErrorMessage = "Plant Description with ID " + nonExistentEntryId + " not found.";
                    final String actualErrorMessage = ((ErrorMessage) response.getRawBody()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldRespondWithStoredEntry() throws PdStoreException {

        final int entryId = 39;

        final PlantDescriptionTracker pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        pdTracker.put(TestUtils.createEntry(entryId));

        final GetPlantDescription handler = new GetPlantDescription(pdTracker);

        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(entryId)))
            .build();

        final MockServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.OK, response.status().orElse(null));
                    final PlantDescriptionEntry returnedEntry = (PlantDescriptionEntry) response.getRawBody();
                    assertEquals(returnedEntry
                        .id(), entryId, 0); // TODO: Add 'equals' method to entries and do a proper comparison?
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldNotAcceptInvalidId() throws PdStoreException {

        final int entryId = 24;
        final String invalidId = "Invalid";

        final PlantDescriptionTracker pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        pdTracker.put(TestUtils.createEntry(entryId));

        final GetPlantDescription handler = new GetPlantDescription(pdTracker);

        final HttpServiceRequest request = new MockRequest.Builder().pathParameters(List.of(invalidId))
            .build();

        final MockServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> assertEquals(HttpStatus.BAD_REQUEST, response.status().orElse(null)))
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

}