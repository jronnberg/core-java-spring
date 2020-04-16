package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import se.arkalix.net.http.service.HttpServiceRequest;

/**
 * An instance of this class embodies a set of requirements placed on a single
 * HttpServiceRequest query parameter, where the value is expected to be a
 * string. Used in conjunction with QueryParamParser for validating and parsing
 * query parameters.
 */
public class StringParameter extends QueryParameter {

    private List<String> legalValues = null;
    private String defaultValue = null;

    /**
     * {@inheritDoc}
     */
    public StringParameter(String name) {
        super(name);
    }

    public StringParameter legalValues(List<String> legalValues) {
        this.legalValues = legalValues;
        return this;
    }

    public StringParameter legalValue(String value) {
        this.legalValues = new ArrayList<String>();
        this.legalValues.add(value);
        return this;
    }

    public StringParameter setDefault(String s) {
        this.defaultValue = s;
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
                parser.report(new ParseError("Missing parameter: " + name + "."));
            }
            if (defaultValue != null) {
                parser.putString(name, defaultValue);
            }
            return;
        }

        for (var param : requiredParameters) {
            param.parse(request, parser, true);
        }

        String value = possibleValue.get();

        if (legalValues != null && !legalValues.contains(value)) {
            parser.report(new ParseError(value + " is not a legal value for parameter " + name + "."));
        }

        parser.putString(name, value);
    }
}