package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

/**
 * An instance of this class represents a single query parameter requriement
 * violation, e.g. an invalid value for a given parameter.
 */
class ParseError {

    private final String message;

    ParseError(String message) {
        this.message = message;
    }

    String getMessage() {
        return message;
    }

}