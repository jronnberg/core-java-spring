package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.Optional;

import se.arkalix.http.service.HttpServiceRequest;

public class BooleanParameter extends QueryParameter {

    private Boolean defaultValue = null; // Use generics?

    public BooleanParameter(String name) {
        super(name);
    }

    public BooleanParameter setDefault(boolean value) {
        this.defaultValue = value;
        return this;
    }

    @Override
    public void parse(HttpServiceRequest request, QueryParamParser parser, boolean required) {

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
            if (defaultValue != null) {
                parser.putBoolean(name, defaultValue);
            }
            return;
        }

        if (possibleValue.isEmpty()) {
            if (required) {
                parser.report(new ParseError("Missing parameter: " + name + "."));
            }
            if (defaultValue != null) {
                parser.putBoolean(name, defaultValue);
            }
            return;
        }

        for (var param : requiredParameters) {
            param.parse(request, parser, true);
        }

        String value = possibleValue.get();
        if (!(value.equals("true") || value.equals("false"))) {
            parser.report(new ParseError(name + " must be true or false, not " + value + "."));
        }

        parser.putBoolean(name, value.equals("true"));
    }
}