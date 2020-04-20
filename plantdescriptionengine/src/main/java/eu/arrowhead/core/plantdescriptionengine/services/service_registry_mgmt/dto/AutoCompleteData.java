package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

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
