package eu.arrowhead.core.plantdescriptionengine.services.monitor.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PdePort;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SystemEntry {

    String systemName();
    Map<String, String> metadata(); // TODO: Make optional
    List<PdePort> ports();
    Map<String, String> systemData(); // TODO: Make optional
    Optional<String> inventoryId();
    Map<String, String> inventoryData(); // TODO: Make optional

    default String asString() {
        return "SystemEntry[systemName=" + systemName() + "]";
    }

}