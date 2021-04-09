package eu.arrowhead.core.plantdescriptionengine.providedservices;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockServiceResponse;
import org.junit.jupiter.api.Test;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.dto.DtoReadException;
import se.arkalix.net.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DtoReadExceptionCatcherTest {

    @Test
    public void shouldReturnAllEntries() {

        final DtoReadExceptionCatcher catcher = new DtoReadExceptionCatcher();

        final String message = "Lorem Ipsum";
        final String value = "ABC";
        final int offset = 0;

        final DtoReadException exception = new DtoReadException(PlantDescriptionEntryDto.class, DtoEncoding.JSON, message, value, offset);


        final MockRequest request = new MockRequest.Builder().body("Body").build();
        final MockServiceResponse response = new MockServiceResponse();

        catcher.handle(exception, request, response);
        assertEquals(HttpStatus.BAD_REQUEST, response.status().orElse(null));
        assertTrue(response.body().isPresent());
        final String body = response.body().get().toString();

        final String expectedError = "ErrorMessage{error='Failed to read eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto from JSON source; the following issue occurred when reading 'ABC' at source offset 0: Lorem Ipsum'}";
        assertEquals(expectedError, body);
    }
}
