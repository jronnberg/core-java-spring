package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockServiceResponse;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmList;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class GetAllPdeAlarmsTest {

    @Test
    public void shouldSortById() {

        final var alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotInDescription("systemNameA");
        alarmManager.raiseSystemNotInDescription("systemNameB");
        alarmManager.raiseSystemNotInDescription("systemNameC");
        final var handler = new GetAllPdeAlarms(alarmManager);

        final HttpServiceRequest ascRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "sort_field", List.of("id"),
                "direction", List.of("ASC")
            ))
            .build();
        final HttpServiceRequest descRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "sort_field", List.of("id"),
                "direction", List.of("DESC")
            ))
            .build();

        final HttpServiceResponse ascResponse = new MockServiceResponse();
        final HttpServiceResponse descResponse = new MockServiceResponse();

        try {
            handler.handle(ascRequest, ascResponse)
                .map(ascendingResult -> {
                    assertEquals(HttpStatus.OK, ascResponse.status().get());

                    final var alarms = (PdeAlarmList) ascResponse.body().get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();

                    for (int i = 1; i < alarms.count(); i++) {
                        final var alarm = alarms.data().get(i);
                        assertTrue(alarm.id() >= previousId);
                        previousId = alarm.id();
                    }
                    return handler.handle(descRequest, descResponse);
                })
                .ifSuccess(descendingResult -> {
                    assertEquals(HttpStatus.OK, descResponse.status().get());

                    final var alarms = (PdeAlarmList) descResponse.body().get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();
                    for (int i = 1; i < alarms.count(); i++) {
                        final var alarm = alarms.data().get(i);
                        assertTrue(alarm.id() <= previousId);
                        previousId = alarm.id();
                    }
                }).onFailure(e -> {
                assertNull(e);
            });
        } catch (final Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldSortByCreatedAt() {

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final var alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotRegistered(Optional.of(systemNameA), null);
        alarmManager.raiseSystemNotRegistered(Optional.of(systemNameB), null);
        alarmManager.raiseSystemNotRegistered(Optional.of(systemNameC), null);
        final var handler = new GetAllPdeAlarms(alarmManager);

        final HttpServiceRequest ascRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "sort_field", List.of("id"),
                "direction", List.of("ASC")
            ))
            .build();
        final HttpServiceRequest descRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "sort_field", List.of("raisedAt"),
                "direction", List.of("DESC")
            ))
            .build();

        final HttpServiceResponse ascResponse = new MockServiceResponse();
        final HttpServiceResponse descResponse = new MockServiceResponse();

        try {
            handler.handle(ascRequest, ascResponse)
                .map(ascendingResult -> {
                    assertEquals(HttpStatus.OK, ascResponse.status().get());

                    final var alarms = (PdeAlarmList) ascResponse.body().get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();

                    for (int i = 1; i < alarms.count(); i++) {
                        final var alarm = alarms.data().get(i);
                        assertTrue(alarm.id() >= previousId);
                        previousId = alarm.id();
                    }
                    return handler.handle(descRequest, descResponse);
                })
                .ifSuccess(descendingResult -> {
                    assertEquals(HttpStatus.OK, descResponse.status().get());

                    final var alarms = (PdeAlarmList) descResponse.body().get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();

                    for (int i = 1; i < alarms.count(); i++) {
                        final var alarm = alarms.data().get(i);
                        assertTrue(alarm.id() <= previousId);
                        previousId = alarm.id();
                    }
                }).onFailure(e -> {
                assertNull(e);
            });
        } catch (final Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectNonBooleans() throws PdStoreException {

        final var handler = new GetAllPdeAlarms(new AlarmManager());
        final String nonBoolean = "Not a boolean";
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "acknowledged", List.of(nonBoolean) // Should be 'true' or 'false'
            ))
            .build();
        final HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<Query parameter 'acknowledged' must be true or false, got '"
                        + nonBoolean + "'.>";
                    String actualErrorMessage = ((ErrorMessage) response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldFilterEntries() throws PdStoreException {

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final var alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotInDescription(systemNameA);
        alarmManager.raiseSystemNotInDescription(systemNameB);
        alarmManager.raiseSystemNotInDescription(systemNameC);

        int alarmId = 0;
        for (final var alarm : alarmManager.getAlarms()) {
            if (alarm.systemName().get().equals(systemNameB)) {
                alarmId = alarm.id();
            }
        }

        alarmManager.setAcknowledged(alarmId, true);
        alarmManager.clearSystemNotInDescription(systemNameC);

        final var handler = new GetAllPdeAlarms(alarmManager);
        final HttpServiceRequest nameRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "systemName", List.of(systemNameA)))
            .build();
        final HttpServiceRequest ackRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "acknowledged", List.of("true")))
            .build();
        final HttpServiceRequest severityRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "severity", List.of("cleared")))
            .build();

        HttpServiceResponse nameResponse = new MockServiceResponse();
        HttpServiceResponse ackResponse = new MockServiceResponse();
        HttpServiceResponse severityResponse = new MockServiceResponse();

        try {
            handler.handle(nameRequest, nameResponse)
                .flatMap(nameResult -> {
                    assertEquals(HttpStatus.OK, nameResponse.status().get());
                    var alarms = (PdeAlarmList) nameResponse.body().get();
                    assertEquals(1, alarms.count());
                    assertEquals(systemNameA, alarms.data().get(0).systemName().get());
                    return handler.handle(ackRequest, ackResponse);
                })
                .flatMap(ackResult -> {
                    assertEquals(HttpStatus.OK, ackResponse.status().get());
                    var alarms = (PdeAlarmList) ackResponse.body().get();
                    assertEquals(1, alarms.count());
                    assertEquals(systemNameB, alarms.data().get(0).systemName().get());
                    return handler.handle(severityRequest, severityResponse);
                }).ifSuccess(severityResult -> {
                assertEquals(HttpStatus.OK, severityResponse.status().get());
                var alarms = (PdeAlarmList) severityResponse.body().get();
                assertEquals(1, alarms.count());
                assertEquals(systemNameC, alarms.data().get(0).systemName().get());
            })
                .onFailure(e -> {
                    assertNull(e);
                });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldPaginate() throws PdStoreException {

        final var alarmManager = new AlarmManager();

        for (int i = 0; i < 10; i++) {
            alarmManager.raiseSystemInactive("System-" + i);
        }

        final var ids = new ArrayList<Integer>();
        for (final var alarm : alarmManager.getAlarms()) {
            ids.add(alarm.id());
        }

        final var handler = new GetAllPdeAlarms(alarmManager);
        final HttpServiceResponse response = new MockServiceResponse();
        final int page = 0;
        final int itemsPerPage = 3;
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
                    assertEquals(HttpStatus.OK, response.status().get());
                    final var alarms = (PdeAlarmList) response.body().get();
                    assertEquals(itemsPerPage, alarms.count());
                    for (int i = 0; i < itemsPerPage; i++) {
                        int index = page * itemsPerPage + i;
                        int alarmId = alarms.data().get(i).id();
                        int expectedId = ids.get(index);
                        assertEquals(expectedId, alarmId);
                    }

                }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectNegativePage() throws PdStoreException {
        final var handler = new GetAllPdeAlarms(new AlarmManager());
        int page = -3;
        final HttpServiceResponse response = new MockServiceResponse();
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "page", List.of(String.valueOf(page)),
                "item_per_page", List.of(String.valueOf(4))
            ))
            .build();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "<Query parameter 'page' must be greater than 0, got " + page + ".>";
                    String actualErrorMessage = ((ErrorMessage) response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                }).onFailure(e -> {
                assertNull(e);
            });
        } catch (Exception e) {
            assertNull(e);
        }
    }

}