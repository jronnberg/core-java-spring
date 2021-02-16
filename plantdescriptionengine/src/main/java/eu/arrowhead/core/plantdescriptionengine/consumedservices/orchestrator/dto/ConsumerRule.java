package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for consumer rule.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface ConsumerRule {
    String consumerSystemId();

    String serviceDefinitionName();

    Optional<String> serviceInterfaceName();

}
