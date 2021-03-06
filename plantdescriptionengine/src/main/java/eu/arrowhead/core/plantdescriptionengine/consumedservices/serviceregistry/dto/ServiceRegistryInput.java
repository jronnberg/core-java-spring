package eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Data Transfer Object (DTO) interface for store entry.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface ServiceRegistryInput {

	String serviceDefinition();
    SrSystem provider();
    String serviceUri();
    String endOfValidity();
    String secure();
    Optional<Map<String, String>> metadata();
    Integer version();
    List<String> interfaces();

}
