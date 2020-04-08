package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.ArrayList;
import java.util.List;

import se.arkalix.http.service.HttpServiceRequest;

/**
 * An instance of this class embodies a set of requirements placed on a single
 * HttpServiceRequest query parameter. Used in conjunction with QueryParamParser
 * for validating and parsing query parameters.
 */
public abstract class QueryParameter {

    protected String name;
    protected List<QueryParameter> requiredParameters = new ArrayList<>();

    public QueryParameter requires(QueryParameter param) {
        requiredParameters.add(param);
        return this;
    }

    /**
     * Constructs an instance of this class.
     * @param name The field name of the query parameter that this instance
     *             refers to.
     */
    public QueryParameter(String name) {
        this.name = name;
    }

    /**
     * Validate and parse the query parameter that this instance corresponds to.
     * If the parameter is present in the request, and it does not violate any
     * of the requirements imposed by this instance, it is stored in the given
     * parser object. Any requirement violations are reported using the parsers
     * {@code report} method.
     * @param request A HTTP service request.
     * @param parser A query parameter parser instance.
     * @param required If true, this method will report an error if the
     *                 parameter is not present in the request.
     */
    public abstract void parse(HttpServiceRequest request, QueryParamParser parser, boolean required);
}