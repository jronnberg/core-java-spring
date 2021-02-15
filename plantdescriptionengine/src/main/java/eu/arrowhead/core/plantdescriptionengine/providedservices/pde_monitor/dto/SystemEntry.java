package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.value.JsonObject;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface SystemEntry {

    String systemId();

    Optional<String> systemName();

    Optional<Map<String, String>> metadata();

    List<PortEntry> ports();

    Optional<JsonObject> systemData();

    Optional<String> inventoryId();

    Optional<JsonObject> inventoryData();

}