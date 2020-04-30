package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Optional;

/**
 * Data Transfer Object (DTO) interface for systems registered in service registry.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SrSystem {
	Integer id(); // TODO: Should this be Optional?
    String systemName();
    String address();
    Integer port();
    String authenticationInfo();
    Optional<String> createdAt();
    Optional<String> updatedAt();


    default String asString() {
        return "SrSystem[systemName=" + systemName() + "]";
    }
}
