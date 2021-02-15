package eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto;

import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for lists of systems registered in service registry.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface ServiceDefinitionList {

    List<ServiceDefinition> data();

    int count();

}
