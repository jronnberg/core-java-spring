package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.util.List;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PlantDescription {
    String plantDescription();
    Optional<Boolean> active();
    Optional<List<Integer>> include();
    List<PdeSystem> systems();
    List<PdeConnection> connections();

    default String asString() {
        return "PlantDescription[plantDescription=" + plantDescription() + "]";
    }
}
