package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Map;
import java.util.Optional;

/**
 * Data Transfer Object (DTO) interface for plant description port entries.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PortEntry {

    String portName();
    String serviceDefinition();
    Optional<Map<String, String>> metadata();

    /*
    Optional<Map<String, String>> systemData();
    Optional<String> inventoryId();
    Optional<Map<String, String>> inventoryData();
    */

    /**
     * Indicates whether this port is used to consume or produce services.
     */
    Optional<Boolean> consumer();

    default String asString() {
        return "PortEntry[portName=" + portName() + ",serviceDefinition=" + serviceDefinition() +
            ",consumer=" + consumer() + "]";
    }
}
