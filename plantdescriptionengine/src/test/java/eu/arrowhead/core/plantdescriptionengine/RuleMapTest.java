package eu.arrowhead.core.plantdescriptionengine;
// package eu.arrowhead.core.plantdescriptionengine;

import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.RuleMap;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.ServiceDefinitionBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.ServiceInterfaceBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystemBuilder;

/**
 * Unit test for the {@link eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.RuleMap;
},
 */
public class RuleMapTest

{
    @Test
    public void shouldStoreRuleIds()
    {
        final RuleMap map = new RuleMap();
        final Instant now = Instant.now();

        var entries = List.of(createRuleA(), createRuleB());
        var ruleList = new StoreEntryListBuilder()
            .count(entries.size())
            .data(entries)
            .build();

        var plantDescriptionEntryID = 99;

        map.put(plantDescriptionEntryID, ruleList);
        var result = map.get(plantDescriptionEntryID);

        assertTrue(result.size() == ruleList.data().size());

        for (var rule : ruleList.data()) {
            assertTrue(result.contains(rule.id()));
        }
    }

    private StoreEntryDto createRuleA() {
        return new StoreEntryBuilder()
            .id(8)
            .serviceDefinition(new ServiceDefinitionBuilder()
                .id(0)
                .createdAt("2020-05-13 13:01:41")
                .updatedAt("2020-05-13 13:01:41")
                .serviceDefinition("Service definition")
                .build())
            .consumerSystem(new SrSystemBuilder()
                .id(0)
                .systemName("Consumer system")
                .address("0.0.0.0")
                .port(01234)
                .build())
            .foreign(false)
            .providerSystem(new SrSystemBuilder()
                .id(1)
                .systemName("Provider system")
                .address("0.0.0.0")
                .port(56789)
                .build())
            .serviceInterface(new ServiceInterfaceBuilder()
                .id(0)
                .interfaceName("Interface name")
                .createdAt("2020-05-13 13:01:41")
                .updatedAt("2020-05-13 13:01:41")
                .build())
            .priority(1)
            .createdAt("2020-05-13 13:01:41")
            .updatedAt("2020-05-13 13:01:41")
            .build();
    }

    private StoreEntryDto createRuleB() {
        return new StoreEntryBuilder()
            .id(9)
            .serviceDefinition(new ServiceDefinitionBuilder()
                .id(0)
                .createdAt("2020-05-13 13:01:41")
                .updatedAt("2020-05-13 13:01:41")
                .serviceDefinition("Service definition")
                .build())
            .consumerSystem(new SrSystemBuilder()
                .id(0)
                .systemName("Consumer system")
                .address("0.0.0.0")
                .port(01234)
                .build())
            .foreign(false)
            .providerSystem(new SrSystemBuilder()
                .id(1)
                .systemName("Provider system")
                .address("0.0.0.0")
                .port(56789)
                .build())
            .serviceInterface(new ServiceInterfaceBuilder()
                .id(0)
                .interfaceName("Interface name")
                .createdAt("2020-05-13 13:01:41")
                .updatedAt("2020-05-13 13:01:41")
                .build())
            .priority(1)
            .createdAt("2020-05-13 13:01:41")
            .updatedAt("2020-05-13 13:01:41")
            .build();
    }
}
