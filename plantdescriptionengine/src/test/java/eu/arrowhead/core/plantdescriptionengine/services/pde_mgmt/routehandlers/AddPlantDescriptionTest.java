package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.InMemoryBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntry;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.utils.Utils;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceResponse;

public class AddPlantDescriptionTest {

    @Test
    public void shouldCreateEntry() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new AddPlantDescription(entryMap);
        final PlantDescription description = Utils.createDescription();
        final HttpServiceResponse response = new MockResponse();
        final MockRequest request = new MockRequest.Builder()
            .body(description)
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertTrue(response.status().isPresent());
                    assertEquals(HttpStatus.CREATED, response.status().get());
                    assertNotNull(response.body());
                    assertTrue(response.body().isPresent());

                    PlantDescriptionEntry entry = (PlantDescriptionEntry)response.body().get();
                    assertTrue(entry.matchesDescription(description));

                    var entryInMap = entryMap.get(entry.id());
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
}