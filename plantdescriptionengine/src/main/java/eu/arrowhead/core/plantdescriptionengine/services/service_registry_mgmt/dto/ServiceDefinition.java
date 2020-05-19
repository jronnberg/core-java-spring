package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for service definition.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface ServiceDefinition {
	Optional<Integer> id();
	String serviceDefinition();
    Optional<String> createdAt();
    Optional<String> updatedAt();

}
