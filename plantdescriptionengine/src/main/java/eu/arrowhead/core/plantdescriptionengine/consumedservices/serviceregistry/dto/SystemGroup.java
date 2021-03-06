package eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

/**
 * Data Transfer Object (DTO) interface for service registry group.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface SystemGroup {

	Integer systemId();
	String systemName();
	String address();
	Integer port();
	List<ServiceDefinition> providerServices();

}
