package eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation;

import java.util.Optional;

import se.arkalix.net.http.service.HttpServiceRequest;


/**
 * An instance of this class embodies a set of requirements placed on a single
 * HttpServiceRequest query parameter, where the value is expected to be a
 * boolean. Used in conjunction with QueryParamParser for validating and parsing
 * query parameters.
 */
public class BooleanParameter extends QueryParameter {

    private Boolean defaultValue = null; // Use generics?

    /**
     * {@inheritDoc}
     */
    public BooleanParameter(String name) {
        super(name);
    }

    /**
     * @param value A default value for the parameter.
     * @return This instance.
     */
    public BooleanParameter setDefault(boolean value) {
        this.defaultValue = value;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void parse(HttpServiceRequest request, QueryParamParser parser, boolean required) {

        Optional<String> possibleValue = request.queryParameter(name);

        if (possibleValue.isEmpty()) {
            if (required) {
                parser.report(new ParseError("Missing parameter '" + name + "'."));
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
            parser.report(new ParseError("Query parameter '" + name + "' must be true or false, got '" + value + "'."));
        }

        parser.putBoolean(name, value.equals("true"));
    }
}