package eu.arrowhead.core.plantdescriptionengine.requestvalidation;

class ParseError {

    private final String message;

    ParseError(String message) {
        this.message = message;
    }

    String getMessage() {
        return message;
    }

}