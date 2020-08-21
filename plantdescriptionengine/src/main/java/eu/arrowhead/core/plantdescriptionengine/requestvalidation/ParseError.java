package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

/**
 * An instance of this class represents a single query parameter requriement
 * violation, e.g. an invalid value for a given parameter.
 */
public class ParseError extends Exception {

    private static final long serialVersionUID = 8823647431608958886L;

    public ParseError(String message) {
        super(message);
    }

}