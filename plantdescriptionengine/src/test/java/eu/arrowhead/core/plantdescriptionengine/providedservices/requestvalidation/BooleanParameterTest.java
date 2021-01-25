package eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation;

import org.junit.jupiter.api.Test;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import se.arkalix.net.http.service.HttpServiceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

public class BooleanParameterTest {

    @Test
    public void shouldParseBooelans() throws ParseError {

        final List<QueryParameter> requiredParameters = List.of(
            new BooleanParameter("smart"),
            new BooleanParameter("tired")
        );

        final List<QueryParameter> acceptedParameters = List.of(
            new BooleanParameter("strong"),
            new BooleanParameter("fat")
        );

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "smart", List.of("true"),
                "tired", List.of("false"),
                "strong", List.of("true"),
                "fat", List.of("false")
            ))
            .build();

        final var parser = new QueryParamParser(requiredParameters, acceptedParameters, request);

        assertEquals(true, parser.getBoolean("smart").get());
        assertEquals(false, parser.getBoolean("tired").get());
        assertEquals(true, parser.getBoolean("strong").get());
        assertEquals(false, parser.getBoolean("fat").get());
    }

    @Test
    public void shouldUseDefaultArgument() throws ParseError {

        final List<QueryParameter> requiredParameters = null;
        final List<QueryParameter> acceptedParameters = List.of(
            new BooleanParameter("good").setDefault(true),
            new BooleanParameter("happy").setDefault(false)
        );

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of())
            .build();

        final var parser = new QueryParamParser(requiredParameters, acceptedParameters, request);

        assertEquals(true, parser.getBoolean("good").get());
        assertEquals(false, parser.getBoolean("happy").get());
    }

    @Test
    public void shouldNonBooleans() throws ParseError {
        final List<QueryParameter> requiredParameters = List.of(
            new BooleanParameter("cool")
        );

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("cool", List.of("128")))
            .build();

        Exception exception = assertThrows(ParseError.class, () -> {
            new QueryParamParser(requiredParameters, null, request);
        });

        assertEquals(
            "<Query parameter 'cool' must be true or false, got '128'.>",
            exception.getMessage()
        );
    }

    @Test
    public void shouldReportMissingParameter() {

        final List<QueryParameter> acceptedParameters = null;
        final List<QueryParameter> requiredParameters = List.of(
            new BooleanParameter("weekends")
        );

        Exception exception = assertThrows(ParseError.class, () -> {
            final HttpServiceRequest request = new MockRequest.Builder()
                .queryParameters(Map.of())
                .build();
            new QueryParamParser(requiredParameters, acceptedParameters, request);
        });
        assertEquals(
            "<Missing parameter 'weekends'.>",
            exception.getMessage()
        );
    }

    @Test
    public void shouldReportMissingDependency() {

        final List<QueryParameter> acceptedParameters = List.of(
            new BooleanParameter("sort")
                .requires(new IntParameter("item_per_page"))
        );
        final List<QueryParameter> requiredParameters = null;

        Exception exception = assertThrows(ParseError.class, () -> {
            final HttpServiceRequest request = new MockRequest.Builder()
                .queryParameters(Map.of("sort", List.of("true")))
                .build();
            new QueryParamParser(requiredParameters, acceptedParameters, request);
        });
        assertEquals(
            "<Missing parameter 'item_per_page'.>",
            exception.getMessage()
        );
    }

}
