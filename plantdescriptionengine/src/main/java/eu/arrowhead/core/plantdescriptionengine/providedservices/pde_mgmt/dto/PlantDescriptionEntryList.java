package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto;

import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for lists of plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface PlantDescriptionEntryList {

    List<PlantDescriptionEntry> data();

    int count();
}
