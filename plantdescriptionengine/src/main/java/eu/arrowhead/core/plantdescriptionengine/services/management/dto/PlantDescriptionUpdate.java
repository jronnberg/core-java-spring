package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PlantDescriptionUpdate {

    String plantDescription();
    boolean active();
    List<Integer> include(); // TODO: Check how this works with Optional
    List<PdeSystem> systems();
    List<PdeConnection> connections();

    default String asString() {
        return "PlantDescriptionUpdate[plantDescription=" + plantDescription() + "]";
    }

}