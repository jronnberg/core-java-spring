package eu.arrowhead.core.plantdescriptionengine.consumedservices.monitorable.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for Inventory IDs.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface InventoryId {
    String id();
}
