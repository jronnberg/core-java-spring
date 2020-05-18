package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SystemEntry {

    Integer systemId();
    Optional<String> systemName();
    Optional<Map<String, String>> metadata();
    List<PortEntry> ports();
    Optional<Map<String, String>> systemData();
    Optional<String> inventoryId();
    Optional<Map<String, String>> inventoryData();

    default String asString() {
        return "SystemEntry[systemName=" + systemName() + "]";
    }

}