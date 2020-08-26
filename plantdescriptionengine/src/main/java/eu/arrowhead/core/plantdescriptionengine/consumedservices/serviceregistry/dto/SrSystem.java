package eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Optional;

/**
 * Data Transfer Object (DTO) interface for systems registered in the Service
 * Registry.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface SrSystem {

	Integer id(); // TODO: Should this be Optional?
    String systemName();
    String address();
    Integer port();
    Optional<String> authenticationInfo(); // TODO: Should this be Optional?
    Optional<String> createdAt();
    Optional<String> updatedAt();

}
