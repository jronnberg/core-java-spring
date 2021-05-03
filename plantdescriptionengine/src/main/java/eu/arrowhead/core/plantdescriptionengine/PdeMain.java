package eu.arrowhead.core.plantdescriptionengine;

public final class PdeMain {

    public static void main(final String[] args) {
        System.out.println("DERP!");
        InventoryId id = new InventoryIdDto.Builder().id("hej").build();
        System.out.println(id);
    }
}