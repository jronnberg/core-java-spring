package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.HashSet;
import java.util.Set;

import se.arkalix.http.service.HttpServiceRequest;

public abstract class QueryParameter {

    protected String name;
    protected String errorMessage;
    protected Set<String> requiredParameters = new HashSet<>();

    public QueryParameter(Builder builder) {
        this.name = builder.name;
        this.requiredParameters = builder.requiredParameters;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public abstract boolean parse(HttpServiceRequest request, QueryParamParser parser, boolean required);

    public static abstract class Builder {

        String name;
        protected Set<String> requiredParameters = new HashSet<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder ifPresentRequire(String param) {
            requiredParameters.add(param);
            return this;
        }

        public abstract QueryParameter build();
    }
}