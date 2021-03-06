package eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for service interface.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface ServiceInterface {

	Integer id();
	String interfaceName();
	String createdAt();
	String updatedAt();

}
