package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.ArrayList;
import java.util.List;

import se.arkalix.http.service.HttpServiceRequest;

public abstract class QueryParameter {

    protected String name;
    protected List<QueryParameter> requiredParameters = new ArrayList<>();

    public QueryParameter requires(QueryParameter param) {
        requiredParameters.add(param);
        return this;
    }

    public QueryParameter(String name) {
        this.name = name;
    }

    public abstract void parse(HttpServiceRequest request, QueryParamParser parser, boolean required);
}