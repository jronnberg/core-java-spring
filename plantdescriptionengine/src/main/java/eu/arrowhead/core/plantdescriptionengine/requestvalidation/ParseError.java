package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

public class ParseError {

    private final String message;

    ParseError(String message) {
        this.message = message;
    }

    String getMessage() {
        return message;
    }

}