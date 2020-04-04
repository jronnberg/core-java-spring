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

    public IntParameter(Builder builder) {
        super(builder);
    }

    @Override
    public boolean parse(HttpServiceRequest request, QueryParamParser parser, boolean required) {

        for (var param : requiredParameters) {
            try {
                Optional<String> value = request.queryParameter(param);
                if (value.isEmpty()) {
                    errorMessage = "Missing parameter " + param + ".";
                    return false;
                }
            } catch (NullPointerException e) { // TODO: This should be unnecessary
                errorMessage = "Missing parameter " + param + ".";
                return false;
            }
        }

        Optional<String> possibleValue = request.queryParameter(name);

        if (possibleValue.isEmpty()) {
            if (!required) {
                return true;
            } else {
                errorMessage = "Missing parameter: " + name + ".";
                return false;
            }
        }

        String value = possibleValue.get();
        if (!isInteger(value)) {
            errorMessage = "Query parameter " + name +
                " must be a valid string, got " + value + ".";
            return false;
        }

        parser.putInt(name, Integer.parseInt(value));
        return true;
    }

    public static class Builder extends eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParameter.Builder {

        public IntParameter build() {
            return new IntParameter(this);
        }

    }
}