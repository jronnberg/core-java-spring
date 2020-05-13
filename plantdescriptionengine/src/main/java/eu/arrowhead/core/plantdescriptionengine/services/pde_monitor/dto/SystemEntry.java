package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.Port;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SystemEntry {

    String systemName();
    Optional<Map<String, String>> metadata();
    List<Port> ports();
    Optional<Map<String, String>> systemData();
    Optional<String> inventoryId();
    Optional<Map<String, String>> inventoryData();

    default String asString() {
        return "SystemEntry[systemName=" + systemName() + "]";
    }

}