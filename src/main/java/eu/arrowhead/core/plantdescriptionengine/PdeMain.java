package eu.arrowhead.core.plantdescriptionengine;

import se.arkalix.codec.json.JsonBoolean;
import se.arkalix.codec.json.JsonObject;
import se.arkalix.codec.json.JsonPair;

public final class PdeMain {

    public static void main(final String[] args) {
        
        var json = new JsonObject(new JsonPair("a", JsonBoolean.TRUE));
        Thing thing = new ThingDto.Builder()
            .stuff(json)
            .build();
        System.out.println(thing);
    }
}