package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto;

import java.util.List;
import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface PlantDescription {
    String plantDescription();
    Optional<Boolean> active();
    Optional<List<Integer>> include();
    List<PdeSystem> systems();
    List<Connection> connections();
}
