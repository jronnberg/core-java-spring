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
    private Map<String, String> stringValues = new HashMap<>();

    private String errorMessage;

    public QueryParamParser(List<QueryParameter> required, List<QueryParameter> accepted) {
        this.required = required;
        this.accepted = accepted;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean parse(HttpServiceRequest request) {
        for (var param : required) {
            if (!param.parse(request, this, true)) {
                errorMessage = param.getErrorMessage();
                return false;
            }
        }
        for (var param : accepted) {
            if (!param.parse(request, this, false)) {
                errorMessage = param.getErrorMessage();
                return false;
            }
        }
        return true;
    }

	public void putInt(String key, Integer value) {
        intValues.put(key, value);
    }

    public Optional<Integer> getInt(String name) {
        Integer i = intValues.get(name);
        return Optional.ofNullable(i);
    }

    public void putString(String key, String value) {
        stringValues.put(key, value);
    }

    public Optional<String> getString(String name) {
        String s = stringValues.get(name);
        return Optional.ofNullable(s);
    }

}