package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.utils.TestUtils;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.BackingStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.backingstore.InMemoryBackingStore;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryList;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.MonitorInfo;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class GetAllPlantDescriptionsTest {

    @Test
    public void shouldRespondWithStoredEntries() throws BackingStoreException {

        final List<Integer> entryIds = List.of(0, 1, 2, 3);
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        for (int id : entryIds) {
            entryMap.put(TestUtils.createEntry(id));
        }

        final var monitorInfo = new MonitorInfo();

        GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        HttpServiceRequest request = new MockRequest();
        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.OK, response.status().get());

                assertTrue(response.body().isPresent());
                var entries = (PlantDescriptionEntryList)response.body().get();
                assertEquals(entryIds.size(), entries.count());
            }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldExtendWithMonitorData() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var monitorInfo = new MonitorInfo();
        final String systemName = "System A";
        final String inventoryId = "system_a_inventory_id";
        final PdeSystemDto system = new PdeSystemBuilder()
            .systemName(systemName)
            .systemId(1)
            .build();
        final Instant now = Instant.now();
        final PlantDescriptionEntryDto entry = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(false)
            .include(new ArrayList<>())
            .systems(List.of(system))
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now)
            .build();

        entryMap.put(entry);
        final Map<String, String> systemData = new HashMap<>();
        systemData.put("a", "1");
        systemData.put("b", "2");
        monitorInfo.putInventoryId(systemName, inventoryId);
        monitorInfo.putSystemData(systemName, systemData);

        GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        HttpServiceRequest request = new MockRequest();
        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.body().isPresent());
                var returnedEntries = (PlantDescriptionEntryList)response.body().get();
                var returnedEntry = returnedEntries.data().get(0);
                var returnedSystem = returnedEntry.systems().get(0);
                assertTrue(returnedSystem.inventoryId().isPresent());
                assertTrue(returnedSystem.systemData().isPresent());
                assertEquals(returnedSystem.inventoryId().get(), inventoryId);
                assertEquals(returnedSystem.systemData().get(), systemData);
            }).onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void shouldSortEntries() throws BackingStoreException {
        final List<Integer> entryIds = List.of(0, 1, 2, 3);
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        for (int id : entryIds) {
            entryMap.put(TestUtils.createEntry(id));
        }

        final var monitorInfo = new MonitorInfo();

        GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "sort_field", List.of("id"),
                "direction", List.of("DESC")
            ))
            .build();
        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.OK, response.status().get());

                assertTrue(response.body().isPresent());
                var entries = (PlantDescriptionEntryList)response.body().get();
                assertEquals(entryIds.size(), entries.count());

                int previousId = entries.data().get(0).id();
                for (int i = 1; i < entries.count(); i++) {
                    var entry = entries.data().get(i);
                    assertTrue(entry.id() <= previousId);
                    previousId = entry.id();
                }
            }).onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectInvalidParameters() throws BackingStoreException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var monitorInfo = new MonitorInfo();

        GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "filter_field", List.of("active")
                // Missing filter_value, invalid request!
            ))
            .build();
        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                assertTrue(response.body().isPresent());
                final String errorMessage = (String)(response.body().get());
                assertEquals("<Missing parameter: filter_value.>", errorMessage);
            }).onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void shouldFilterEntries() throws BackingStoreException {

        final List<Integer> entryIds = List.of(0, 1, 2);
        final int activeEntryId = 3;
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        for (int id : entryIds) {
            entryMap.put(TestUtils.createEntry(id));
        }

        final var monitorInfo = new MonitorInfo();
        final Instant now = Instant.now();
        entryMap.put(new PlantDescriptionEntryBuilder()
            .id(activeEntryId)
            .plantDescription("Plant Description 1B")
            .active(true)
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now)
            .build());

        GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "filter_field", List.of("active"),
                "filter_value", List.of("true")
            ))
            .build();
        HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.OK, response.status().get());

                assertTrue(response.body().isPresent());
                var entries = (PlantDescriptionEntryList)response.body().get();
                assertEquals(1, entries.count());
                assertEquals(entries.data().get(0).id(), activeEntryId);
            }).onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    // TODO: Test pagination as well.
}