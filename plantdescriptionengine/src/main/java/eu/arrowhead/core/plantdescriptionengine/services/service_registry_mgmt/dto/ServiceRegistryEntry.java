package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
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
public interface ServiceRegistryEntry {
	Integer id();
	ServiceDefinition serviceDefinition();
    SrSystem provider();
    String serviceUri();
    Optional<String> endOfValidity(); // TODO: Should this be Optional?
    String secure();
    Optional<Map<String, String>> metadata();
    Integer version();
    List<ServiceInterface> interfaces();
    String createdAt();
    String updatedAt();


    default String asString() {
        return "ServiceRegistryEntry[serviceDefinition=" + serviceDefinition() + ", provider=" + provider().systemName() + "]";
    }
}
