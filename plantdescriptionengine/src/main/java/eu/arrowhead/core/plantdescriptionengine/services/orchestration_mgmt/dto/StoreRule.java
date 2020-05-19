package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Map;
import java.util.Optional;

/**
 * Data Transfer Object (DTO) interface for store rule.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface StoreRule {
	String serviceDefinitionName();
	Integer consumerSystemId();
    Optional<Map<String, String>> attribute();

    ProviderSystem providerSystem();
    Cloud cloud();
    String serviceInterfaceName();

    Integer priority();
}
