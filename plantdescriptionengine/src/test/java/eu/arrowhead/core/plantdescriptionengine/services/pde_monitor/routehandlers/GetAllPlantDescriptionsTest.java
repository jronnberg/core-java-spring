package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.MonitorInfo;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PlantDescriptionEntryList;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class GetAllPlantDescriptionsTest {

    @Test
    public void shouldRespondWithStoredEntries() throws BackingStoreException {

        final List<Integer> entryIds = List.of(0, 1, 2, 3);
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        for (final int id : entryIds) {
            entryMap.put(TestUtils.createEntry(id));
        }

        final var monitorInfo = new MonitorInfo();

        final GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        final HttpServiceRequest request = new MockRequest();
        final HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.OK, response.status().get());

                assertTrue(response.body().isPresent());
                final var entries = (PlantDescriptionEntryList)response.body().get();
                assertEquals(entryIds.size(), entries.count());
            }).onFailure(e -> {
                assertNull(e);
            });
        } catch (final Exception e) {
            assertNull(e);
        }
    }

    // @Test TODO: Implement and enable this test!
    public void shouldExtendWithMonitorData() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var monitorInfo = new MonitorInfo();
        final String systemName = "System A";
        final String inventoryId = "system_a_inventory_id";
        final PdeSystemDto system = new PdeSystemBuilder()
            .systemName(systemName)
            .systemId("system_a")
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

        // monitorInfo.putInventoryId(systemName, inventoryId);
        // monitorInfo.putSystemData(systemName, systemData);

        final GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        final HttpServiceRequest request = new MockRequest();
        final HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.body().isPresent());
                final var returnedEntries = (PlantDescriptionEntryList)response.body().get();
                final var returnedEntry = returnedEntries.data().get(0);
                final var returnedSystem = returnedEntry.systems().get(0);
                assertTrue(returnedSystem.inventoryId().isPresent());
                assertTrue(returnedSystem.systemData().isPresent());
                assertEquals(returnedSystem.inventoryId().get(), inventoryId);
                assertEquals(returnedSystem.systemData().get(), systemData);
            }).onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        } catch (final Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void shouldSortEntries() throws BackingStoreException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        final Instant createdAt1 = Instant.parse("2020-05-27T14:48:00.00Z");
        final Instant createdAt2 = Instant.parse("2020-06-27T14:48:00.00Z");
        final Instant createdAt3 = Instant.parse("2020-07-27T14:48:00.00Z");

        final Instant updatedAt1 = Instant.parse("2020-08-01T14:48:00.00Z");
        final Instant updatedAt2 = Instant.parse("2020-08-03T14:48:00.00Z");
        final Instant updatedAt3 = Instant.parse("2020-08-02T14:48:00.00Z");

        final PlantDescriptionEntryDto entry1 = new PlantDescriptionEntryBuilder()
            .id(32)
            .plantDescription("Plant Description 1")
            .active(false)
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .createdAt(createdAt1)
            .updatedAt(updatedAt1)
            .build();
        final PlantDescriptionEntryDto entry2 = new PlantDescriptionEntryBuilder()
            .id(2)
            .plantDescription("Plant Description 2")
            .active(false)
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .createdAt(createdAt2)
            .updatedAt(updatedAt2)
            .build();
        final PlantDescriptionEntryDto entry3 = new PlantDescriptionEntryBuilder()
            .id(8)
            .plantDescription("Plant Description 3")
            .active(false)
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .createdAt(createdAt3)
            .updatedAt(updatedAt3)
            .build();

        entryMap.put(entry1);
        entryMap.put(entry2);
        entryMap.put(entry3);

        final int numEntries = entryMap.getListDto().count();

        final var monitorInfo = new MonitorInfo();

        final GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        final HttpServiceRequest idDescendingRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "sort_field", List.of("id"),
                "direction", List.of("DESC")
            ))
            .build();
        final HttpServiceRequest creationAscendingRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "sort_field", List.of("createdAt"),
                "direction", List.of("ASC")
            ))
            .build();
        final HttpServiceRequest updatesDescendingRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "sort_field", List.of("updatedAt"),
                "direction", List.of("DESC")
            ))
            .build();
        final HttpServiceResponse response1 = new MockResponse();
        final HttpServiceResponse response2 = new MockResponse();
        final HttpServiceResponse response3 = new MockResponse();

        try {
            handler.handle(idDescendingRequest, response1)
            .flatMap(result -> {
                assertTrue(response1.status().isPresent());
                assertEquals(HttpStatus.OK, response1.status().get());

                assertTrue(response1.body().isPresent());
                final var entries = (PlantDescriptionEntryList)response1.body().get();
                assertEquals(numEntries, entries.count());

                float previousId = entries.data().get(0).id();
                for (int i = 1; i < entries.count(); i++) {
                    final var entry = entries.data().get(i);
                    assertTrue(entry.id() <= previousId);
                    previousId = entry.id();
                }

                return handler.handle(creationAscendingRequest, response2);
            })
            .flatMap(result -> {
                assertTrue(response2.status().isPresent());
                assertEquals(HttpStatus.OK, response2.status().get());

                assertTrue(response2.body().isPresent());
                final var entries = (PlantDescriptionEntryList)response2.body().get();
                assertEquals(numEntries, entries.count());

                Instant previousTimestamp = entries.data().get(0).createdAt();
                for (int i = 1; i < entries.count(); i++) {
                    final var entry = entries.data().get(i);
                    assertTrue(entry.createdAt().compareTo(previousTimestamp) >= 0);
                    previousTimestamp = entry.createdAt();
                }

                return handler.handle(updatesDescendingRequest, response3);
            })
            .ifSuccess(result -> {
                assertTrue(response3.status().isPresent());
                assertEquals(HttpStatus.OK, response3.status().get());

                assertTrue(response3.body().isPresent());
                final var entries = (PlantDescriptionEntryList)response3.body().get();
                assertEquals(numEntries, entries.count());

                Instant previousTimestamp = entries.data().get(0).updatedAt();
                for (int i = 1; i < entries.count(); i++) {
                    final var entry = entries.data().get(i);
                    assertTrue(entry.updatedAt().compareTo(previousTimestamp) < 0);
                    previousTimestamp = entry.updatedAt();
                }
            }).onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        } catch (final Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectInvalidParameters() throws BackingStoreException {
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var monitorInfo = new MonitorInfo();

        final GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "filter_field", List.of("active")
                // Missing filter_value, invalid request!
            ))
            .build();
        final HttpServiceResponse response = new MockResponse();

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
        } catch (final Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void shouldFilterEntries() throws BackingStoreException {

        final List<Integer> entryIds = List.of(0, 1, 2);
        final int activeEntryId = 3;
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        for (final int id : entryIds) {
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

        final GetAllPlantDescriptions handler = new GetAllPlantDescriptions(monitorInfo, entryMap);
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "filter_field", List.of("active"),
                "filter_value", List.of("true")
            ))
            .build();
        final HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.OK, response.status().get());

                assertTrue(response.body().isPresent());
                final var entries = (PlantDescriptionEntryList)response.body().get();
                assertEquals(1, entries.count());
                assertEquals(entries.data().get(0).id(), activeEntryId, 0);
            }).onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        } catch (final Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void shouldPaginate() throws BackingStoreException {

        final List<Integer> entryIds = Arrays.asList(32, 11, 25, 3, 24, 35);
        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        for (int id : entryIds) {
            entryMap.put(TestUtils.createEntry(id));
        }

        final var handler = new GetAllPlantDescriptions(new MonitorInfo(), entryMap);
        final HttpServiceResponse response = new MockResponse();
        final int page = 1;
        final int itemsPerPage = 2;
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "sort_field", List.of("id"),
                "page", List.of(String.valueOf(page)),
                "item_per_page", List.of(String.valueOf(itemsPerPage))
            ))
            .build();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.OK, response.status().get());

                assertTrue(response.body().isPresent());
                var entries = (PlantDescriptionEntryList)response.body().get();
                assertEquals(itemsPerPage, entries.count());

                // Sort the entry ID:s, so that their order will match that of
                // the response data.
                Collections.sort(entryIds);

                for (int i = 0; i < itemsPerPage; i++) {
                    int index = page * itemsPerPage + i;
                    assertEquals((int)entryIds.get(index), entries.data().get(i).id(), 0);
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
    public void shouldRejectNegativePage() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());
        final var handler = new GetAllPlantDescriptions(new MonitorInfo(), entryMap);
        final HttpServiceResponse response = new MockResponse();
        final int page = -1;
        final int itemsPerPage = 2;
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "page", List.of(String.valueOf(page)),
                "item_per_page", List.of(String.valueOf(itemsPerPage))
            ))
            .build();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.BAD_REQUEST, response.status().get());

                assertTrue(response.body().isPresent());
                String expectedErrMsg = "<Query parameter 'page' must be greater than 0, got " + page + ".>";
                assertEquals(expectedErrMsg, response.body().get());

            }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRequireItemPerPage() throws BackingStoreException {

        final var entryMap = new PlantDescriptionEntryMap(new InMemoryBackingStore());

        final var handler = new GetAllPlantDescriptions(new MonitorInfo(), entryMap);
        final HttpServiceResponse response = new MockResponse();
        final int page = 4;
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "page", List.of(String.valueOf(page))
            ))
            .build();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertTrue(response.status().isPresent());
                assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                assertTrue(response.body().isPresent());
                String expectedErrMsg = "<Missing parameter: item_per_page.>";
                assertEquals(expectedErrMsg, response.body().get());

            }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            assertNull(e);
        }
    }

}