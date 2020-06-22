package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import static org.junit.Assert.assertNotNull;
import java.util.List;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryListBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.dto.StoreEntryListDto;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap.backingstore.InMemoryBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap.backingstore.RuleBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulemap.backingstore.RuleBackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.ServiceDefinitionBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.ServiceInterfaceBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystemBuilder;

/**
 * Unit test for the
 * {@link eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.RuleMap}.
 */
public class RuleMapTest {

    private StoreEntryDto createRule(int id) {
        return new StoreEntryBuilder()
            .id(id)
            .serviceDefinition(new ServiceDefinitionBuilder()
                .id(0).createdAt("2020-05-13 13:01:41")
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
                .id(1).systemName("Provider system")
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

    private StoreEntryListDto createRuleList() {
        var entries = List.of(createRule(0), createRule(1));
        return new StoreEntryListBuilder()
            .count(entries.size())
            .data(entries)
            .build();
    }

    @Test
    public void shouldStoreRuleIds() throws RuleBackingStoreException {
        final RuleBackingStore backingStore = new InMemoryBackingStore();
        final RuleMap map = new RuleMap(backingStore);
        StoreEntryListDto ruleList = createRuleList();
        int plantDescriptionEntryId = 99;

        map.put(plantDescriptionEntryId, ruleList);
        var storedIds = map.get(plantDescriptionEntryId);

        assertNotNull(storedIds);
        assertEquals(ruleList.data().size(), storedIds.size());
        for (var rule : ruleList.data()) {
            assertTrue(storedIds.contains(rule.id()));
        }
    }

    @Test
    public void shouldRemoveRuleIds() throws RuleBackingStoreException {
        final RuleBackingStore backingStore = new InMemoryBackingStore();
        final RuleMap map = new RuleMap(backingStore);
        StoreEntryListDto ruleList = createRuleList();
        int plantDescriptionEntryID = 36;

        map.put(plantDescriptionEntryID, ruleList);
        map.remove(plantDescriptionEntryID);
        var storedIds = map.get(plantDescriptionEntryID);

        assertNull(storedIds);
    }
}
