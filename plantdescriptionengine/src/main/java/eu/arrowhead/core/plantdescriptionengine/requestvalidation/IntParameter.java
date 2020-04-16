package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.Optional;
import java.util.Scanner;

import se.arkalix.net.http.service.HttpServiceRequest;

/**
 * An instance of this class embodies a set of requirements placed on a single
 * HttpServiceRequest query parameter, where the value is expected to be an
 * integer. Used in conjunction with QueryParamParser for validating and parsing
 * query parameters.
 */
public class IntParameter extends QueryParameter {

    /**
     * @return True if the provided string is a base 10 integer.
     */
    private static boolean isInteger(String s) {
        int radix = 10;
        Scanner scanner = new Scanner(s.trim());

        if (!scanner.hasNextInt(radix)) {
            return false;
        }
        scanner.nextInt(radix);
        return !scanner.hasNext();
    }

    /**
    * {@inheritDoc}
    */
    public IntParameter(String name) {
        super(name);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void parse(HttpServiceRequest request, QueryParamParser parser, boolean required) {

        Optional<String> possibleValue = request.queryParameter(name);

        if (possibleValue.isEmpty()) {
            if (required) {
                parser.report(new ParseError("Missing parameter: " + name + "."));
            }
            return;
        }

        for (var param : requiredParameters) {
            param.parse(request, parser, true);
        }

        String value = possibleValue.get();

        if (!isInteger(value)) {
            parser.report(new ParseError("Query parameter " + name +
                " must be a valid string, got " + value + "."));
        }

        if (!parser.hasError()) {
            parser.putInt(name, Integer.parseInt(value));
        }
    }
}