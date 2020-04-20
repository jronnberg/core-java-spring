package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for service interface.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface ServiceInterface {
	Integer id();
	String interfaceName();
	String createdAt();
	String updatedAt();

    default String asString() {
        return "ServiceInterface[interfaceName=" + interfaceName() + "]";
    }

}
