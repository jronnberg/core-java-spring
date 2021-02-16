package eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import java.util.List;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for auto complete data.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface AutoCompleteData {
    List<ServiceInterface> interfaceList();

    List<ServiceDefinition> serviceList();

    List<SrSystem> systemList();
}
