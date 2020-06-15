package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescription;

/**
 * Class for validating the Plant Descriptions.
 */
public class PlantDescriptionValidator {

    private final List<String> errors = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param pd A Plant Description to be validated.
     */
    public PlantDescriptionValidator(PlantDescription pd) {
        boolean producerFound = false;
        boolean consumerFound = false;

        for (var connection : pd.connections()) {
            final var producer = connection.producer();
            final var consumer = connection.consumer();

            final String producerId = producer.systemId();
            final String consumerId = consumer.systemId();

            for (var system : pd.systems()) {
                if (producerId.equals(system.systemId())) {
                    producerFound = true;
                    if (!system.hasPort(producer.portName())) {
                        errors.add("Connection refers to the missing producer port '" + producer.portName() + "'");
                    }
                } else if (consumerId.equals(system.systemId())) {
                    consumerFound = true;
                    if (!system.hasPort(consumer.portName())) {
                        errors.add("Connection refers to the missing consumer port '" + consumer.portName() + "'");
                    }
                }
            }

            if (!producerFound) {
                errors.add("A connection refers to the missing system '" + producerId + "'");
            }
            if (!consumerFound) {
                errors.add("A connection refers to the missing system '" + consumerId + "'");
            }
        }
    }

    /**
     * @return A human-readable description of any errors in the
     *         Plant Description.
     */
	public String getErrorMessage() {
        List<String> errorMessages = new ArrayList<>();
        for (String error : errors) {
            errorMessages.add("<" + error + ">");
        }
        return String.join(", ", errorMessages);
    }

    public boolean hasError() {
        return errors.size() > 0;
    }

}