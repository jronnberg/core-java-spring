package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystem;
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

        for (var system : pd.systems()) {
            ensureUniquePorts(system);
        }
    }

    /**
     * Ensures that the given system's ports are all unique.
     *
     * The PDE must be able to differentiate between the ports of a system.
     * When multiple ports share the same serviceDefinition, they must have
     * different metadata. This method ensures that this property holds.
     *
     *  TODO: Check that metadata is unique
     *
     * @param system The system whose ports will be validated.
     */
    private void ensureUniquePorts(final PdeSystem system) {

        Map<String, Integer> portsPerService = new HashMap<>();
        Set<String> portNames = new HashSet<>();

        // Map serviceDefinitions to lists of metadata:
        Map<String, List<Map<String, String>>> metadataPerService = new HashMap<>();

        for (var port : system.ports()) {

            String portName = port.portName();
            if (portNames.contains(portName)) {
                errors.add("Duplicate port name '" + portName + "' in system '" + system.systemId() + "'");
            }

            portNames.add(portName);

            final String serviceDefinition = port.serviceDefinition();
            Integer numPorts = portsPerService.getOrDefault(serviceDefinition, 0);
            portsPerService.put(serviceDefinition, numPorts + 1);

            if (!metadataPerService.containsKey(serviceDefinition)) {
                metadataPerService.put(serviceDefinition, new ArrayList<>());
            }

            if (port.metadata().isPresent()) {
                metadataPerService.get(serviceDefinition).add(port.metadata().get());
            }
        }

        for (String serviceDefinition : portsPerService.keySet()) {

            Integer numPorts = portsPerService.getOrDefault(serviceDefinition, 0);
            Integer numMetadata = metadataPerService.get(serviceDefinition).size();

            // Ensure that there is metadata to differentiate between ports when
            // multiple ports share service definition:
            if (numPorts > numMetadata + 1) {
                errors.add(system.systemId() + " has multiple ports with service definition '" +
                    serviceDefinition + "' without metadata.");
            }

            // Ensure that the metadata is unique within each serviceDefinition:
            List<Map<String, String>> serviceMetadata = metadataPerService.get(serviceDefinition);
            if (serviceMetadata.size() > 1) {
                var uniqueMetadata = new HashSet<Map<String, String>>(serviceMetadata);
                if (uniqueMetadata.size() < serviceMetadata.size()) {
                    errors.add(system.systemId() + " has duplicate metadata for ports with service definition '" +
                        serviceDefinition + "'");
                }
            }
        }
    }

    /**
     * @return A human-readable description of any errors in the Plant Description.
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