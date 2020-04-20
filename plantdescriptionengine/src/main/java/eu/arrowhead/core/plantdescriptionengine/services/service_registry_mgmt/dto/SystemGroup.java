package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for service registry group.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SystemGroup {
	Integer systemId();
	String systemName();
	String address();
	Integer port();
	List<ServiceDefinition> providerServices();

    default String asString() {
        return "SystemGroup[systemName=" + systemName() + "]";
    }
}
