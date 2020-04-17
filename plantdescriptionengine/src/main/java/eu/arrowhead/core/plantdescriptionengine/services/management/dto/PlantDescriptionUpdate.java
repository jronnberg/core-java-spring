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
public interface PlantDescriptionUpdate {

    Optional<String> plantDescription();
    Optional<Boolean> active();
    Optional<List<PdeSystem>> systems();
    Optional<List<PdeConnection>> connections();
    Optional<List<Integer>> include();

    default String asString() {
        return "PlantDescriptionUpdate[plantDescription=" + plantDescription().orElse("N/A") + "]";
    }

}