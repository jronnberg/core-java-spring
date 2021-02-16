package eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation;

import se.arkalix.net.http.service.HttpServiceRequest;

import java.util.*;

/**
 * Class for parsing and validating the query parameters of HttpServiceRequests.
 */
public class QueryParamParser {

    private final List<QueryParameter> required;
    private final List<QueryParameter> accepted;

    private final Map<String, Integer> intValues = new HashMap<>();
    private final Map<String, Boolean> boolValues = new HashMap<>();
    private final Map<String, String> stringValues = new HashMap<>();

    private final List<ParseError> errors = new ArrayList<>();

    /**
     * Constructs an instance of this class.
     * The query parameters of the provided request are immediately parsed and
     * validated according to the query parameter requirements specified by the
     * two first arguments.
     * <p>
     * All of the parameters specified in {@code required} must be present, and
     * all of their requirements fulfilled, for the request to be considered
     * valid.
     * <p>
     * The parameters in {@code accepted} may be left out of the request, but if
     * present, must fulfill their requirements.
     * <p>
     * If the parameters are invalid, a {@code #ParseError} is thrown.
     * <p>
     * If the parameters are valid, their values will be accessible via the
     * methods {@code getInt}, {@code getBoolean} and {@code getString}.
     *
     * @param required A list of all query parameters that are required for this
     *                 request to be considered valid, with specific constraints
     *                 for each one.
     * @param accepted A list of accepted query parameters
     * @param request  The head and body of an incoming HTTP request.
     */
    public QueryParamParser(
        List<QueryParameter> required, List<QueryParameter> accepted, HttpServiceRequest request
    ) throws ParseError {

        if (required == null) {
            required = new ArrayList<>();
        }

        if (accepted == null) {
            accepted = new ArrayList<>();
        }

        this.required = required;
        this.accepted = accepted;
        parse(request);

        if (hasError()) {
            throw getCompoundError();
        }
    }

    public boolean hasError() {
        return errors.size() > 0;
    }

    /**
     * Stores information about a single query parameter requirement violation.
     *
     * @param error The error to report.
     */
    void report(ParseError error) {
        errors.add(error);
    }

    /**
     * Validates and parses the query parameters of the given request.
     *
     * @param request The request to parse.
     */
    private void parse(HttpServiceRequest request) {
        for (var param : required) {
            param.parse(request, this, true);
        }
        for (var param : accepted) {
            param.parse(request, this, false);
        }
    }

    void putInt(String key, Integer value) {
        intValues.put(key, value);
    }

    public Optional<Integer> getInt(String name) {
        Integer i = intValues.get(name);
        return Optional.ofNullable(i);
    }

    void putBoolean(String key, Boolean value) {
        boolValues.put(key, value);
    }

    public Optional<Boolean> getBoolean(String name) {
        Boolean value = boolValues.get(name);
        return Optional.ofNullable(value);
    }

    void putString(String key, String value) {
        stringValues.put(key, value);
    }

    public Optional<String> getString(String name) {
        String s = stringValues.get(name);
        return Optional.ofNullable(s);
    }

    /**
     * @return A compound error describing all individual errors that occurred
     * during parsing.
     */
    public ParseError getCompoundError() {
        List<String> errorMessages = new ArrayList<>();
        for (ParseError error : errors) {
            errorMessages.add("<" + error.getMessage() + ">");
        }
        return new ParseError(String.join(", ", errorMessages));
    }

}