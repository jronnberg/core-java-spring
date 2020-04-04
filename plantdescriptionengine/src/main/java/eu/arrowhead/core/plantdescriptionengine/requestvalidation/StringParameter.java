package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.List;
import java.util.Optional;

import se.arkalix.http.service.HttpServiceRequest;

public class StringParameter extends QueryParameter {

    private List<String> legalValues = null;

    public StringParameter(String name) {
        super(name);
    }

    public StringParameter in(List<String> legalValues) {
        this.legalValues = legalValues;
        return this;
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

        if (legalValues != null && !legalValues.contains(value)) {
            parser.report(new ParseError(value + " is not a legal value for parameter " + name + "."));
        }

        parser.putString(name, value);
    }
}