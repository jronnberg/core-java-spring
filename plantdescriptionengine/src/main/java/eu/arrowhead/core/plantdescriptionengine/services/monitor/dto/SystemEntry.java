package eu.arrowhead.core.plantdescriptionengine.services.monitor.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import eu.arrowhead.core.plantdescriptionengine.services.management.dto.PdePort;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SystemEntry {

    String systemName();
    // TODO: Add metadata field
    List<PdePort> ports();
    // TODO: Add systemData field
    Optional<String> inventoryId();
    // TODO: Add inventoryData field

    default String asString() {
        return "SystemEntry[systemName=" + systemName() + "]";
    }

}