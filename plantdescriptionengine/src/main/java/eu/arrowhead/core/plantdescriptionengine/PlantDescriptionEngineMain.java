package eu.arrowhead.core.plantdescriptionengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import se.arkalix.dto.DtoWriteException;

public class PlantDescriptionEngineMain {

    public static PlantDescriptionDto createDescription() {

        PdePortDto serviceDiscoveryPort = new PdePortBuilder()
            .portName("service_discovery")
            .serviceDefinition("Service Discovery")
            .consumer(false)
            .build();
        System.out.println("* " + serviceDiscoveryPort.asString());

        PdePortDto authorizationPort = new PdePortBuilder()
            .portName("service_discovery")
            .serviceDefinition("Service Discovery")
            .consumer(false)
            .build();
        System.out.println("* " + serviceDiscoveryPort.asString());

        PdeConnectionEndPointDto consumer = new PdeConnectionEndPointBuilder()
            .systemName("Authorization")
            .portName("service_discovery")
            .build();
        System.out.println("* " + consumer.asString());

        PdeConnectionEndPointDto producer = new PdeConnectionEndPointBuilder()
            .systemName("Service Registry")
            .portName("service_discovery")
            .build();
        System.out.println("* " + producer.asString());

        PdeConnectionDto connection = new PdeConnectionBuilder()
            .consumer(consumer)
            .producer(producer)
            .build();
        System.out.println("* " + connection.asString());

        PdeSystemDto serviceRegistrySystem = new PdeSystemBuilder()
            .systemName("Service Registry")
            .ports(Arrays.asList(serviceDiscoveryPort))
            .build();
        System.out.println("* " + serviceRegistrySystem.asString());

        PdeSystemDto authorizationSystem = new PdeSystemBuilder()
            .systemName("Authorization")
            .ports(Arrays.asList(authorizationPort))
            .build();
        System.out.println("* " + authorizationSystem.asString());

        PlantDescriptionDto description = new PlantDescriptionBuilder()
            .id(1).plantDescription("ArrowHead core")
            .systems(Arrays.asList(serviceRegistrySystem, authorizationSystem))
            .connections(Arrays.asList(connection))
            .build();
        System.out.println("* " + description.asString());

        return description;
    }

    private static void writeToFile(List<PlantDescriptionDto> descriptions) throws DtoWriteException, IOException {
        FileOutputStream out = new FileOutputStream(new File("plant-description.json"));
        DtoWriter writer = new DtoWriter(out);
        for (var description : descriptions) {
            description.writeJson(writer);
        }
        out.close();
    }

    public static void main(final String[] args) {

        System.out.println("Creating a plant description:");

        final PlantDescriptionDto description = createDescription();

        System.out.println("Writing description to file...");

        try {
            writeToFile(Arrays.asList(description));
        } catch (DtoWriteException | IOException e) {
            e.printStackTrace();
            System.exit(74);
        }

        System.out.println("Done.");

    }
}
