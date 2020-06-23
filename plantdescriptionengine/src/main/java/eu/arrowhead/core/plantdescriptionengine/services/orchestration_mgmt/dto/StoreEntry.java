package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Map;
import java.util.Optional;

import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.ServiceDefinition;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.ServiceInterface;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystem;

/**
 * Data Transfer Object (DTO) interface for store entry.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface StoreEntry {
	Integer id();
	ServiceDefinition serviceDefinition();
    SrSystem consumerSystem();
    Boolean foreign();

    SrSystem providerSystem();
    Optional<ProviderCloud> providerCloud(); // TODO: Should this be Optional?
    ServiceInterface serviceInterface();

    Integer priority();
    Optional<Map<String, String>> attribute();
    String createdAt();
    String updatedAt();
}