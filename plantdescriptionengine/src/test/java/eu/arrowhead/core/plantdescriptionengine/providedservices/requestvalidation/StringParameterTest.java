package eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation;

import org.junit.jupiter.api.Test;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import se.arkalix.net.http.service.HttpServiceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

public class StringParameterTest {

    @Test
    public void shouldParseParameters() throws ParseError {

        final List<QueryParameter> acceptedParameters = List.of(new StringParameter("episode")
            .legalValues(List.of("A New Hope", "The Empire Strikes Back", "Return of the Jedi"))
            .requires(new IntParameter("score")));

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "episode", List.of("The Empire Strikes Back"),
                "score", List.of("10")
            ))
            .build();

        final var parser = new QueryParamParser(null, acceptedParameters, request);

        assertEquals("The Empire Strikes Back", parser.getString("episode").orElse(null));
        assertEquals(10, parser.getInt("score").orElse(null));
    }

    @Test
    public void shouldUseDefaultArgument() throws ParseError {

        final List<QueryParameter> acceptedParameters = List.of(new StringParameter("episode")
            .legalValues(List.of("A New Hope", "The Empire Strikes Back", "Return of the Jedi"))
            .setDefault(("A new Hope")));

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of())
            .build();

        final var parser = new QueryParamParser(null, acceptedParameters, request);

        assertEquals("A new Hope", parser.getString("episode").orElse(null));
    }

    @Test
    public void shouldReportMissingParameter() {

        final List<QueryParameter> requiredParameters = List.of(
            new StringParameter("sort_field")
                .legalValues(List.of("id", "createdAt", "updatedAt"))
        );

        Exception exception = assertThrows(ParseError.class, () -> {
            final HttpServiceRequest request = new MockRequest.Builder()
                .queryParameters(Map.of())
                .build();
            new QueryParamParser(requiredParameters, null, request);
        });
        assertEquals(
            exception.getMessage(),
            "<Missing parameter 'sort_field'.>"
        );
    }

    @Test
    public void shouldReportMissingDependency() {

        final List<QueryParameter> acceptedParameters = List.of(
            new StringParameter("name")
                .requires(new IntParameter("age"))
        );

        Exception exception = assertThrows(ParseError.class, () -> {
            final HttpServiceRequest request = new MockRequest.Builder()
                .queryParameters(Map.of("name", List.of("Alice")))
                .build();
            new QueryParamParser(null, acceptedParameters, request);
        });
        assertEquals(
            "<Missing parameter 'age'.>",
            exception.getMessage()
        );
    }

    @Test
    public void shouldOnlyAcceptLegalValues() {

        final List<QueryParameter> requiredParameters = List.of(
            new StringParameter("episode")
                .legalValues(List.of(
                    "A New Hope",
                    "The Empire Strikes Back",
                    "Return of the Jedi")));


        Exception exception = assertThrows(ParseError.class, () -> {
            final HttpServiceRequest request = new MockRequest.Builder()
                .queryParameters(Map.of(
                    "episode", List.of("The Rise of Skywalker")
                ))
                .build();
            new QueryParamParser(requiredParameters, null, request);
        });
        assertEquals(
            exception.getMessage(),
            "<The Rise of Skywalker is not a legal value for parameter episode.>"
        );
    }

}
