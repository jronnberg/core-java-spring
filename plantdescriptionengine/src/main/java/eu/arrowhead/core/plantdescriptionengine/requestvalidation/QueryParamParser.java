package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.arkalix.http.service.HttpServiceRequest;

public class QueryParamParser {

    private List<QueryParameter> required = new ArrayList<>();
    private List<QueryParameter> accepted = new ArrayList<>();

    private Map<String, Integer> intValues = new HashMap<>();
    private Map<String, Boolean> boolValues = new HashMap<>();
    private Map<String, String> stringValues = new HashMap<>();

    private List<ParseError> errors = new ArrayList<ParseError>();

    public QueryParamParser(List<QueryParameter> required, List<QueryParameter> accepted, HttpServiceRequest request) {

        if (required == null) {
            required = new ArrayList<>();
        }

        if (accepted == null) {
            accepted = new ArrayList<>();
        }

        this.required = required;
        this.accepted = accepted;
        parse(request);
    }

    public boolean hasError() {
        return errors.size() > 0;
    }

    void report(ParseError error) {
        errors.add(error);
    }

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

	public String getErrorMessage() {
        List<String> errorMessages = new ArrayList<>();
        for (ParseError error : errors) {
            errorMessages.add("<" + error.getMessage() + ">");
        }
        return String.join(", ", errorMessages);
	}

}