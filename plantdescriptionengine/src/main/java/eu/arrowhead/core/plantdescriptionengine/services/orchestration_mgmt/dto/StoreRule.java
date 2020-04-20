package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Map;
import java.util.Optional;

/**
 * Data Transfer Object (DTO) interface for store rule.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface StoreRule {
	String serviceDefinitionName();
	Integer consumerSystemId();
    Optional<Map<String, String>> attribute();

    ProviderSystem providerSystem();
    Cloud Cloud();
    String serviceInterfaceName();

    Integer priority();

    default String asString() {
        return "StoreRule[serviceDefinitionName=" + serviceDefinitionName() + ", consumerSystemId=" + consumerSystemId() + "]";
    }
}
