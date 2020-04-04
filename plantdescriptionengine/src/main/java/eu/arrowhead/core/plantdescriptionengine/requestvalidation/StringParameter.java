package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.List;
import java.util.Optional;

import se.arkalix.http.service.HttpServiceRequest;

public class StringParameter extends QueryParameter {

    private List<String> legalValues = null;

    public StringParameter(Builder builder) {
        super(builder);
        legalValues = builder.legalValues;
    }

    @Override
    public boolean parse(HttpServiceRequest request, QueryParamParser parser, boolean required) {

        for (var param : requiredParameters) {
            if (request.queryParameter(param).isEmpty()) {
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

        if (legalValues != null && !legalValues.contains(value)) {
            errorMessage = value + " is not a legal value for parameter " + name + ".";
        }

        parser.putString(name, value);
        return true;
    }

    public static class Builder extends eu.arrowhead.core.plantdescriptionengine.requestvalidation.QueryParameter.Builder {

        private List<String> legalValues = null;

        public Builder in(List<String> legalValues) {
            this.legalValues = legalValues;
            return this;
        }

        public StringParameter build() {
            return new StringParameter(this);
        }
    }
}