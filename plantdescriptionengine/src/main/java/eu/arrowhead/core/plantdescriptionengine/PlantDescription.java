package eu.arrowhead.core.plantdescriptionengine;

import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
interface PlantDescription {

    int id();
    String plantDescription();
    boolean active();
    List<Integer> include();
    List<PdeSystem> systems();
    List<PdeConnection> connections();

    default String asString() {
        return "PlantDescription[id=" + id() + ",plantDescription=" + plantDescription() + ",active=" + active() + "]";
    }
}
