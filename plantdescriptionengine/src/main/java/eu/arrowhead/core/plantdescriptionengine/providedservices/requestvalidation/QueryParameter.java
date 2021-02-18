package eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation;

import se.arkalix.net.http.service.HttpServiceRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An instance of this class embodies a set of requirements placed on a single
 * HttpServiceRequest query parameter. Used in conjunction with QueryParamParser
 * for validating and parsing query parameters.
 */
public class QueryParameter {

    protected abstract static class Builder<T extends Builder<T>> {
        protected String name = null;
        protected final List<QueryParameter> requiredParameters = new ArrayList<>();

        public abstract T self();

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T requires(QueryParameter param) {
            requiredParameters.add(param);
            return self();
        }
    }

    /**
     * Class constructor.
     * @param builder Builder instance used to configure the instance.
     */
    protected QueryParameter(Builder<?> builder) {

        Objects.requireNonNull(builder.name, "No name has been set on builder instance.");

        this.name = builder.name;
        this.requiredParameters.addAll(builder.requiredParameters);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param name The field name of the query parameter that this instance
     *             refers to.
     */
    public QueryParameter(String name) {
        this.name = name;
    }

    protected final String name;
    protected final List<QueryParameter> requiredParameters = new ArrayList<>();

    /**
     * @param param A query parameter that must be present if this one is.
     * @return This instance.
     */
    public QueryParameter requires(QueryParameter param) {
        requiredParameters.add(param);
        return this;
    }

    /**
     * Validate and parse the query parameter that this instance corresponds to.
     * If the parameter is present in the request, and it does not violate any
     * of the requirements imposed by this instance, it is stored in the given
     * parser object. Any requirement violations are reported using the parser's
     * {@code report} method.
     *
     * @param request  A HTTP service request.
     * @param parser   A query parameter parser instance.
     * @param required If true, this method will report an error if the
     *                 parameter is not present in the request.
     */
    public void parse(HttpServiceRequest request, QueryParamParser parser, boolean required) {
    }
}