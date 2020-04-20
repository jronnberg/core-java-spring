package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for provider systems.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface ProviderSystem {
    String systemName();
    String address();
    Integer port();

    default String asString() {
        return "ProviderSystem[systemName=" + systemName() + "]";
    }
}
