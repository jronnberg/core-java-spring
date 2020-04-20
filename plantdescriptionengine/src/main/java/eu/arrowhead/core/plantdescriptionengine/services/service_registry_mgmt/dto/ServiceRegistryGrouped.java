package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for service registry grouped.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface ServiceRegistryGrouped {
	AutoCompleteData autoCompleteData();
	List<ServiceGroup> servicesGroupedByServiceDefinition();
	List<SystemGroup> servicesGroupedBySystems();
}
