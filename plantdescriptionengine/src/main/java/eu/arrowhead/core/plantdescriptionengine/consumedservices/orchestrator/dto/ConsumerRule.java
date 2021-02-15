package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto;

import static se.arkalix.dto.DtoEncoding.JSON;

import java.util.Optional;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

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
