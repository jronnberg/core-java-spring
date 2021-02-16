package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmList;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockServiceResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
                    assertEquals(HttpStatus.OK, ascResponse.status().orElse(null));

                    assertTrue(ascResponse.body().isPresent());
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
                    assertEquals(HttpStatus.OK, descResponse.status().orElse(null));

                    assertTrue(descResponse.body().isPresent());
                    final var alarms = (PdeAlarmList) descResponse.body().get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();
                    for (int i = 1; i < alarms.count(); i++) {
                        final var alarm = alarms.data().get(i);
                        assertTrue(alarm.id() <= previousId);
                        previousId = alarm.id();
                    }
                }).onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldSortByCreatedAt() {

        final String systemIdA = "Sys-A";
        final String systemIdB = "Sys-B";
        final String systemIdC = "Sys-C";

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final var alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotRegistered(systemIdA, systemNameA);
        alarmManager.raiseSystemNotRegistered(systemIdB, systemNameB);
        alarmManager.raiseSystemNotRegistered(systemIdC, systemNameC);
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
                    assertEquals(HttpStatus.OK, ascResponse.status().orElse(null));

                    assertTrue(ascResponse.body().isPresent());
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
                    assertEquals(HttpStatus.OK, descResponse.status().orElse(null));

                    assertTrue(descResponse.body().isPresent());
                    final var body = descResponse.body();
                    assertTrue(body.isPresent());
                    final var alarms = (PdeAlarmList) body.get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();

                    for (int i = 1; i < alarms.count(); i++) {
                        final var alarm = alarms.data().get(i);
                        assertTrue(alarm.id() <= previousId);
                        previousId = alarm.id();
                    }
                }).onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectNonBooleans() {

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
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().orElse(null));
                    String expectedErrorMessage = "<Query parameter 'acknowledged' must be true or false, got '"
                        + nonBoolean + "'.>";
                    final var body = response.body();
                    assertTrue(body.isPresent());
                    String actualErrorMessage = ((ErrorMessage) body.get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                }).onFailure(Assertions::assertNull);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldFilterEntries() {

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final var alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotInDescription(systemNameA);
        alarmManager.raiseSystemNotInDescription(systemNameB);
        alarmManager.raiseSystemNotInDescription(systemNameC);

        int alarmId = 0;
        for (final var alarm : alarmManager.getAlarms()) {
            assertTrue(alarm.systemName().isPresent());
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
                    assertEquals(HttpStatus.OK, nameResponse.status().orElse(null));
                    assertTrue(nameResponse.body().isPresent());
                    var alarms = (PdeAlarmList) nameResponse.body().get();
                    assertEquals(1, alarms.count());
                    assertEquals(systemNameA, alarms.data().get(0).systemName().orElse(null));
                    return handler.handle(ackRequest, ackResponse);
                })
                .flatMap(ackResult -> {
                    assertEquals(HttpStatus.OK, ackResponse.status().orElse(null));
                    assertTrue(ackResponse.body().isPresent());
                    var alarms = (PdeAlarmList) ackResponse.body().get();
                    assertEquals(1, alarms.count());
                    assertEquals(systemNameB, alarms.data().get(0).systemName().orElse(null));
                    return handler.handle(severityRequest, severityResponse);
                }).ifSuccess(severityResult -> {
                assertEquals(HttpStatus.OK, severityResponse.status().orElse(null));
                assertTrue(severityResponse.body().isPresent());
                var alarms = (PdeAlarmList) severityResponse.body().get();
                assertEquals(1, alarms.count());
                assertEquals(systemNameC, alarms.data().get(0).systemName().orElse(null));
            })
                .onFailure(Assertions::assertNull);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldPaginate() {

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
        final int page = 2;
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
                    assertEquals(HttpStatus.OK, response.status().orElse(null));
                    assertTrue(response.body().isPresent());
                    final var alarms = (PdeAlarmList) response.body().get();
                    assertEquals(itemsPerPage, alarms.count());
                    for (int i = 0; i < itemsPerPage; i++) {
                        int index = page * itemsPerPage + i;
                        int alarmId = alarms.data().get(i).id();
                        int expectedId = ids.get(index);
                        assertEquals(expectedId, alarmId);
                    }

                }).onFailure(Assertions::assertNull);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void shouldRejectNegativePage() {
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
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().orElse(null));
                    String expectedErrorMessage = "<Query parameter 'page' must be greater than 0, got " + page + ".>";
                    assertTrue(response.body().isPresent());
                    String actualErrorMessage = ((ErrorMessage) response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                }).onFailure(Assertions::assertNull);
        } catch (Exception e) {
            assertNull(e);
        }
    }

}