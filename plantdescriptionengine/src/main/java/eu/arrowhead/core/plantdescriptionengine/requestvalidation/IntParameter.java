package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.Optional;
import java.util.Scanner;

import se.arkalix.http.service.HttpServiceRequest;

public class IntParameter extends QueryParameter {

    private static boolean isInteger(String s) {
        int radix = 10;
        Scanner scanner = new Scanner(s.trim());

        if (!scanner.hasNextInt(radix)) {
            return false;
        }
        scanner.nextInt(radix);
        return !scanner.hasNext();
    }

    public IntParameter(String name) {
        super(name);
    }

    @Override
    public void parse(HttpServiceRequest request, QueryParamParser parser, boolean required) {

        for (var param : requiredParameters) {
            param.parse(request, parser, true);
        }

        Optional<String> possibleValue;

        try {
            // TODO: Find out why 'request.queryParameter' throws
            // NullPointerExceptions. If this is an implementation error,
            // remove this try/catch when it has been fixed. Otherwise,
            // find a nicer way to work around it.
            possibleValue = request.queryParameter(name);
        } catch (NullPointerException e) {
            if (required) {
                parser.report(new ParseError("Missing parameter: " + name + "."));
            }
            return;
        }

        if (possibleValue.isEmpty()) {
            if (required) {
                parser.report(new ParseError("Missing parameter: " + name + "."));
            }
            return;
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