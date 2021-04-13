package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmDto;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class GetAllPdeAlarmsTest {

    @Test
    public void shouldSortById() {

        final AlarmManager alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotInDescription("systemNameA", null);
        alarmManager.raiseSystemNotInDescription("systemNameB", null);
        alarmManager.raiseSystemNotInDescription("systemNameC", null);
        final GetAllPdeAlarms handler = new GetAllPdeAlarms(alarmManager);

        final HttpServiceRequest ascRequest = new MockRequest.Builder()
            .queryParameters(Map.of("sort_field", List.of("id"), "direction", List.of("ASC")))
            .build();
        final HttpServiceRequest descRequest = new MockRequest.Builder()
            .queryParameters(Map.of("sort_field", List.of("id"), "direction", List.of("DESC")))
            .build();

        final HttpServiceResponse ascResponse = new MockServiceResponse();
        final HttpServiceResponse descResponse = new MockServiceResponse();

        try {
            handler.handle(ascRequest, ascResponse)
                .map(ascendingResult -> {
                    assertEquals(HttpStatus.OK, ascResponse.status().orElse(null));

                    assertTrue(ascResponse.body().isPresent());
                    final PdeAlarmList alarms = (PdeAlarmList) ascResponse.body().get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();

                    for (int i = 1; i < alarms.count(); i++) {
                        final PdeAlarm alarm = alarms.data().get(i);
                        assertTrue(alarm.id() >= previousId);
                        previousId = alarm.id();
                    }
                    return handler.handle(descRequest, descResponse);
                })
                .ifSuccess(descendingResult -> {
                    assertEquals(HttpStatus.OK, descResponse.status().orElse(null));

                    assertTrue(descResponse.body().isPresent());
                    final PdeAlarmList alarms = (PdeAlarmList) descResponse.body().get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();
                    for (int i = 1; i < alarms.count(); i++) {
                        final PdeAlarm alarm = alarms.data().get(i);
                        assertTrue(alarm.id() <= previousId);
                        previousId = alarm.id();
                    }
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldSortByRaisedAt() {

        final String systemIdA = "Sys-A";
        final String systemIdB = "Sys-B";
        final String systemIdC = "Sys-C";

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final AlarmManager alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotRegistered(systemIdA, systemNameA, null);
        alarmManager.raiseSystemNotRegistered(systemIdB, systemNameB, null);
        alarmManager.raiseSystemNotRegistered(systemIdC, systemNameC, null);
        final GetAllPdeAlarms handler = new GetAllPdeAlarms(alarmManager);

        final HttpServiceRequest ascRequest = new MockRequest.Builder()
            .queryParameters(Map.of("sort_field", List.of("raisedAt"), "direction", List.of("ASC")))
            .build();
        final HttpServiceRequest descRequest = new MockRequest.Builder()
            .queryParameters(
                Map.of("sort_field", List.of("raisedAt"), "direction", List.of("DESC"))
            )
            .build();

        final HttpServiceResponse ascResponse = new MockServiceResponse();
        final HttpServiceResponse descResponse = new MockServiceResponse();

        try {
            handler.handle(ascRequest, ascResponse)
                .map(ascendingResult -> {
                    assertEquals(HttpStatus.OK, ascResponse.status().orElse(null));

                    assertTrue(ascResponse.body().isPresent());
                    final PdeAlarmList alarms = (PdeAlarmList) ascResponse.body().get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();

                    for (int i = 1; i < alarms.count(); i++) {
                        final PdeAlarm alarm = alarms.data().get(i);
                        assertTrue(alarm.id() >= previousId);
                        previousId = alarm.id();
                    }
                    return handler.handle(descRequest, descResponse);
                })
                .ifSuccess(descendingResult -> {
                    assertEquals(HttpStatus.OK, descResponse.status().orElse(null));

                    assertTrue(descResponse.body().isPresent());
                    final Optional<Object> body = descResponse.body();
                    assertTrue(body.isPresent());
                    final PdeAlarmList alarms = (PdeAlarmList) body.get();
                    assertEquals(3, alarms.count());

                    int previousId = alarms.data().get(0).id();

                    for (int i = 1; i < alarms.count(); i++) {
                        final PdeAlarm alarm = alarms.data().get(i);
                        assertTrue(alarm.id() <= previousId);
                        previousId = alarm.id();
                    }
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldSortByClearedAt() {

        final String systemIdA = "Sys-A";
        final String systemIdB = "Sys-B";
        final String systemIdC = "Sys-C";

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final AlarmManager alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotRegistered(systemIdC, systemNameC, null);

        int systemCAlarmId = alarmManager.getAlarms().get(0).id();

        alarmManager.raiseSystemNotRegistered(systemIdA, systemNameA, null);
        alarmManager.raiseSystemNotRegistered(systemIdB, systemNameB, null);

        alarmManager.clearAlarm(systemCAlarmId);

        final GetAllPdeAlarms handler = new GetAllPdeAlarms(alarmManager);

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("sort_field", List.of("clearedAt"), "direction", List.of("ASC")))
            .build();

        final HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(ascendingResult -> {
                    assertEquals(HttpStatus.OK, response.status().orElse(null));
                    assertTrue(response.body().isPresent());
                    final PdeAlarmList alarms = (PdeAlarmList) response.body().get();
                    final PdeAlarm firstAlarm = alarms.data().get(0);
                    assertEquals(systemNameC, firstAlarm.systemName().orElse(null));
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldSortByUpdatedAt() {

        final String systemIdA = "Sys-A";
        final String systemIdB = "Sys-B";
        final String systemIdC = "Sys-C";

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final AlarmManager alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotRegistered(systemIdB, systemNameB, null);
        int systemBAlarmId = alarmManager.getAlarms().get(0).id();

        alarmManager.raiseSystemNotRegistered(systemIdA, systemNameA, null);
        alarmManager.raiseSystemNotRegistered(systemIdC, systemNameC, null);

        alarmManager.setAcknowledged(systemBAlarmId, true); // This changes the 'updatedAt' field

        final GetAllPdeAlarms handler = new GetAllPdeAlarms(alarmManager);

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("sort_field", List.of("updatedAt"), "direction", List.of("DESC")))
            .build();

        final HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(ascendingResult -> {
                    assertEquals(HttpStatus.OK, response.status().orElse(null));
                    assertTrue(response.body().isPresent());
                    final PdeAlarmList alarms = (PdeAlarmList) response.body().get();
                    final PdeAlarm firstAlarm = alarms.data().get(0);
                    assertEquals(systemNameB, firstAlarm.systemName().orElse(null));
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldRejectNonBooleans() {

        final GetAllPdeAlarms handler = new GetAllPdeAlarms(new AlarmManager());
        final String nonBoolean = "Not a boolean";
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("acknowledged", List.of(nonBoolean) // Should be 'true' or 'false'
            ))
            .build();
        final HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().orElse(null));
                    final String expectedErrorMessage = "<Query parameter 'acknowledged' must be true or false, got '"
                        + nonBoolean + "'.>";
                    final Optional<Object> body = response.body();
                    assertTrue(body.isPresent());
                    final String actualErrorMessage = ((ErrorMessage) body.get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldPaginate() {

        final AlarmManager alarmManager = new AlarmManager();

        for (int i = 0; i < 10; i++) {
            alarmManager.raiseSystemInactive("System-" + i);
        }

        final ArrayList<Integer> ids = new ArrayList<>();
        for (final PdeAlarm alarm : alarmManager.getAlarms()) {
            ids.add(alarm.id());
        }

        final GetAllPdeAlarms handler = new GetAllPdeAlarms(alarmManager);
        final HttpServiceResponse response = new MockServiceResponse();
        final int page = 2;
        final int itemsPerPage = 3;
        final HttpServiceRequest request = new MockRequest.Builder().queryParameters(Map.of("sort_field", List.of("id"),
            "page", List.of(String.valueOf(page)), "item_per_page", List.of(String.valueOf(itemsPerPage)))).build();

        try {
            handler.handle(request, response).ifSuccess(result -> {
                assertEquals(HttpStatus.OK, response.status().orElse(null));
                assertTrue(response.body().isPresent());
                final PdeAlarmList alarms = (PdeAlarmList) response.body().get();
                assertEquals(itemsPerPage, alarms.count());
                for (int i = 0; i < itemsPerPage; i++) {
                    final int index = page * itemsPerPage + i;
                    final int alarmId = alarms.data().get(i).id();
                    final int expectedId = ids.get(index);
                    assertEquals(expectedId, alarmId);
                }

            }).onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldRejectNegativePage() {
        final GetAllPdeAlarms handler = new GetAllPdeAlarms(new AlarmManager());
        final int page = -3;
        final HttpServiceResponse response = new MockServiceResponse();
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("page", List.of(String.valueOf(page)), "item_per_page", List.of(String.valueOf(4))))
            .build();

        try {
            handler.handle(request, response).ifSuccess(result -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.status().orElse(null));
                final String expectedErrorMessage = "<Query parameter 'page' must be greater than 0, got " + page + ".>";
                assertTrue(response.body().isPresent());
                final String actualErrorMessage = ((ErrorMessage) response.body().get()).error();
                assertEquals(expectedErrorMessage, actualErrorMessage);
            }).onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldFilterBySystemName() {
        final String systemIdA = "Sys-A";
        final String systemIdB = "Sys-B";
        final String systemIdC = "Sys-C";

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final AlarmManager alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotRegistered(systemIdA, systemNameA, null);
        alarmManager.raiseSystemNotRegistered(systemIdB, systemNameB, null);
        alarmManager.raiseSystemNotRegistered(systemIdC, systemNameC, null);

        final GetAllPdeAlarms handler = new GetAllPdeAlarms(alarmManager);

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("systemName", List.of(systemNameA)))
            .build();

        final HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(ascendingResult -> {
                    assertEquals(HttpStatus.OK, response.status().orElse(null));

                    assertTrue(response.body().isPresent());
                    final PdeAlarmList alarms = (PdeAlarmList) response.body().get();
                    assertEquals(1, alarms.count());

                    assertEquals(systemNameA, alarms.data().get(0).systemName().orElse(null));
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldFilterBySeverity() {
        final String systemIdA = "Sys-A";
        final String systemIdB = "Sys-B";

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final AlarmManager alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotRegistered(systemIdA, systemNameA, null);
        alarmManager.raiseSystemNotRegistered(systemIdB, systemNameB, null);
        alarmManager.raiseSystemInactive(systemNameC);
        alarmManager.clearSystemInactive(systemNameC);

        final GetAllPdeAlarms handler = new GetAllPdeAlarms(alarmManager);

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("severity", List.of("warning")))
            .build();

        final HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(ascendingResult -> {
                    assertEquals(HttpStatus.OK, response.status().orElse(null));

                    assertTrue(response.body().isPresent());
                    final PdeAlarmList alarms = (PdeAlarmList) response.body().get();
                    assertEquals(2, alarms.count());

                    assertEquals(systemNameA, alarms.data().get(0).systemName().orElse(null));
                    assertEquals(systemNameB, alarms.data().get(1).systemName().orElse(null));
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void shouldFilterAcknowledged() {

        final String systemNameA = "System A";
        final String systemNameB = "System B";
        final String systemNameC = "System C";

        final AlarmManager alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotInDescription(systemNameA, null);
        alarmManager.raiseSystemNotInDescription(systemNameB, null);
        alarmManager.raiseSystemNotInDescription(systemNameC, null);

        final List<PdeAlarmDto> alarms = alarmManager.getAlarms();

        alarmManager.setAcknowledged(alarms.get(0).id(), true);
        alarmManager.setAcknowledged(alarms.get(1).id(), true);

        final GetAllPdeAlarms handler = new GetAllPdeAlarms(alarmManager);

        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of("acknowledged", List.of("false")))
            .build();

        final HttpServiceResponse response = new MockServiceResponse();

        try {
            handler.handle(request, response)
                .ifSuccess(ascendingResult -> {
                    assertEquals(HttpStatus.OK, response.status().orElse(null));

                    assertTrue(response.body().isPresent());
                    final PdeAlarmList result = (PdeAlarmList) response.body().get();
                    assertEquals(1, result.count());

                    assertEquals(systemNameC, result.data().get(0).systemName().orElse(null));
                })
                .onFailure(Assertions::assertNull);
        } catch (final Exception e) {
            fail();
        }
    }

}