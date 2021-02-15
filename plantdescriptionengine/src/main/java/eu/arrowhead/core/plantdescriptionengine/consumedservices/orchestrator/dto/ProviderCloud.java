package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for provider cloud.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface ProviderCloud {
    Integer id();

    String operator();

    String name();

    String authenticationInfo();

    Boolean secure();

    Boolean neighour();

    Boolean ownCloud();

    String createdAt();

    String updatedAt();
}
