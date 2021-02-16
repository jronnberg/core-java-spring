package eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation;

import org.junit.jupiter.api.Test;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import se.arkalix.net.http.service.HttpServiceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

public class IntParameterTest {

    @Test
    public void shouldParseIntegers() throws ParseError {

        final List<QueryParameter> requiredParameters = List.of(
            new IntParameter("a"),
            new IntParameter("b")
        );

        final List<QueryParameter> acceptedParameters = List.of(
            new IntParameter("c"),
            new IntParameter("d")
        );

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "a", List.of("0"),
                "b", List.of("1"),
                "c", List.of("126"),
                "d", List.of("99999")
            ))
            .build();

        final var parser = new QueryParamParser(requiredParameters, acceptedParameters, request);

        assertEquals(0, parser.getInt("a").get());
        assertEquals(1, parser.getInt("b").get());
        assertEquals(126, parser.getInt("c").get());
        assertEquals(99999, parser.getInt("d").get());
    }

    @Test
    public void shouldRejectNonInteger() {
        final List<QueryParameter> requiredParameters = List.of(
            new IntParameter("weight")
        );

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("weight", List.of("heavy")))
            .build();

        Exception exception = assertThrows(ParseError.class, () -> new QueryParamParser(requiredParameters, null, request));

        assertEquals(
            "<Query parameter 'weight' must be a valid integer, got 'heavy'.>",
            exception.getMessage()
        );
    }

    @Test
    public void shouldRejectInvalidInteger() {
        final List<QueryParameter> requiredParameters = List.of(
            new IntParameter("weight")
        );

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("weight", List.of("123 test")))
            .build();

        Exception exception = assertThrows(ParseError.class, () -> new QueryParamParser(requiredParameters, null, request));

        assertEquals(
            "<Query parameter 'weight' must be a valid integer, got '123 test'.>",
            exception.getMessage()
        );
    }

    @Test
    public void shouldRejectTooSmallValues() {
        final List<QueryParameter> requiredParameters = List.of(
            new IntParameter("a").min(38),
            new IntParameter("b").min(38),
            new IntParameter("c").min(38)
        );

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(
                Map.of(
                    "a", List.of("39"),
                    "b", List.of("38"),
                    "c", List.of("37")
                )
            )
            .build();

        Exception exception = assertThrows(ParseError.class, () -> new QueryParamParser(requiredParameters, null, request));

        assertEquals(
            "<Query parameter 'c' must be greater than 38, got 37.>",
            exception.getMessage()
        );
    }

    @Test
    public void shouldReportMissingParameter() {

        final List<QueryParameter> acceptedParameters = null;
        final List<QueryParameter> requiredParameters = List.of(
            new IntParameter("height")
        );

        Exception exception = assertThrows(ParseError.class, () -> {
            final HttpServiceRequest request = new MockRequest.Builder()
                .queryParameters(Map.of())
                .build();
            new QueryParamParser(requiredParameters, null, request);
        });
        assertEquals(
            "<Missing parameter 'height'.>",
            exception.getMessage()
        );
    }

    @Test
    public void shouldReportMissingDependency() {

        final List<QueryParameter> acceptedParameters = List.of(
            new IntParameter("a")
                .requires(new IntParameter("b"))
        );
        final List<QueryParameter> requiredParameters = null;

        Exception exception = assertThrows(ParseError.class, () -> {
            final HttpServiceRequest request = new MockRequest.Builder()
                .queryParameters(Map.of("a", List.of("95")))
                .build();
            new QueryParamParser(null, acceptedParameters, request);
        });
        assertEquals(
            "<Missing parameter 'b'.>",
            exception.getMessage()
        );
    }
}
