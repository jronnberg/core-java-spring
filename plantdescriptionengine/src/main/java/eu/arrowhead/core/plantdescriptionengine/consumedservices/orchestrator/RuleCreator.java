package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.RuleSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreRuleBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.dto.StoreRuleDto;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.Connection;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystem;
import se.arkalix.dto.DtoWritable;

/**
 * Class used for creating Orchestrator rules based on connections found in
 * Plant Descriptions.
 */
public class RuleCreator {

    private final PlantDescriptionTracker pdTracker;
    private final boolean isSecure;

    public RuleCreator(PlantDescriptionTracker pdTracker, boolean secure) {
        Objects.requireNonNull(pdTracker, "Expected Plant Description Tracker.");
        this.pdTracker = pdTracker;
        this.isSecure = secure;
    }

    /**
     * Create an Orchestrator rule to be passed to the Orchestrator.
     *
     * @param connection A connection between a producer and consumer system present
     *                   in a Plant Description Entry.
     * @return An Orchestrator rule that embodies the specified connection.
     */
    StoreRuleDto createRule(Connection connection) {

        Objects.requireNonNull(connection, "Expected a connection");

        final String consumerId = connection.consumer().systemId();
        final String providerId = connection.producer().systemId();

        final PdeSystem consumer = pdTracker.getSystem(consumerId);
        final PdeSystem provider = pdTracker.getSystem(providerId);

        String producerPort = connection.producer().portName();
        String consumerPort = connection.consumer().portName();

        final Map<String, String> providerMetadata = provider.portMetadata(producerPort);
        final Map<String, String> consumerMetadata = consumer.portMetadata(consumerPort);

        String serviceDefinition = pdTracker.getServiceDefinition(producerPort);

        var builder = new StoreRuleBuilder()
            .consumerSystem(new RuleSystemBuilder()
                .systemName(consumer.systemName().orElse(null))
                .metadata(providerMetadata)
                .build())
            .providerSystem(new RuleSystemBuilder()
                .systemName(provider.systemName().orElse(null))
                .metadata(consumerMetadata)
                .build())
            .serviceDefinitionName(serviceDefinition);

        if (isSecure) {
            builder.serviceInterfaceName("HTTP-SECURE-JSON");
        } else {
            builder.serviceInterfaceName("HTTP-INSECURE-JSON");
        }

        return builder.build();
    }

    public List<DtoWritable> createRules() {
        List<DtoWritable> rules = new ArrayList<>();
        var connections = pdTracker.getActiveConnections();

        for (var connection : connections) {
            var rule = createRule(connection);
            if (rule != null) {
                rules.add(rule);
            }
        }

        return rules;
    }

}
