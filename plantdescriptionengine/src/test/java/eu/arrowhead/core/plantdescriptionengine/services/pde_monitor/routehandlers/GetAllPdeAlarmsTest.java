package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.alarmmanager.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmList;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class GetAllPdeAlarmsTest {

    @Test
    public void shouldSortById() {

        final String systemNameA = "System A";
        final String systemIdB = "system-b";
        final String systemIdC = "system-c";

        final var alarmManager = new AlarmManager();
        Locator.setAlarmManager(alarmManager);

        alarmManager.raiseAlarmBySystemName(systemNameA, AlarmManager.Cause.systemNotRegistered);
        alarmManager.raiseAlarmBySystemId(systemIdB, AlarmManager.Cause.systemNotInDescription);
        alarmManager.raiseAlarmBySystemId(systemIdC, AlarmManager.Cause.systemNotInDescription);
        final var handler = new GetAllPdeAlarms();

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

        final HttpServiceResponse ascResponse = new MockResponse();
        final HttpServiceResponse descResponse = new MockResponse();

        try {
            handler.handle(ascRequest, ascResponse)
            .map(ascendingResult -> {
                assertEquals(HttpStatus.OK, ascResponse.status().get());

                final var alarms = (PdeAlarmList)ascResponse.body().get();
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

                final var alarms = (PdeAlarmList)descResponse.body().get();
                assertEquals(3, alarms.count());

                int previousId = alarms.data().get(0).id();
                for (int i = 1; i < alarms.count(); i++) {
                    final var alarm = alarms.data().get(i);
                    assertTrue(alarm.id() <= previousId);
                    previousId = alarm.id();
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
    public void shouldSortByCreatedAt() {

        final String systemNameA = "System A";
        final String systemIdB = "system-b";
        final String systemIdC = "system-c";

        final var alarmManager = new AlarmManager();
        Locator.setAlarmManager(alarmManager);

        alarmManager.raiseAlarmBySystemName(systemNameA, AlarmManager.Cause.systemNotRegistered);
        alarmManager.raiseAlarmBySystemId(systemIdB, AlarmManager.Cause.systemNotInDescription);
        alarmManager.raiseAlarmBySystemId(systemIdC, AlarmManager.Cause.systemNotInDescription);
        final var handler = new GetAllPdeAlarms();

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

        final HttpServiceResponse ascResponse = new MockResponse();
        final HttpServiceResponse descResponse = new MockResponse();

        try {
            handler.handle(ascRequest, ascResponse)
            .map(ascendingResult -> {
                assertEquals(HttpStatus.OK, ascResponse.status().get());

                final var alarms = (PdeAlarmList)ascResponse.body().get();
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

                final var alarms = (PdeAlarmList)descResponse.body().get();
                assertEquals(3, alarms.count());

                int previousId = alarms.data().get(0).id();

                for (int i = 1; i < alarms.count(); i++) {
                    final var alarm = alarms.data().get(i);
                    assertTrue(alarm.id() <= previousId);
                    previousId = alarm.id();
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
    public void shouldRejectNonBooleans() throws PdStoreException {

        Locator.setAlarmManager(new AlarmManager());
        final var handler = new GetAllPdeAlarms();
        final String nonBoolean = "Not a boolean";
        final HttpServiceRequest request = new MockRequest.Builder()
            .queryParameters(Map.of(
                "acknowledged", List.of(nonBoolean) // Should be 'true' or 'false'
            ))
            .build();
        final HttpServiceResponse response = new MockResponse();

        try {
            handler.handle(request, response)
            .ifSuccess(result -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                String expectedErrorMessage = "<'acknowledged' must be true or false, not '" + nonBoolean + "'.>";
                String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                assertEquals(expectedErrorMessage, actualErrorMessage);
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
    public void shouldFilterEntries() throws PdStoreException {

        final String systemNameA = "System A";
        final String systemIdB = "system-b";
        final String systemIdC = "system-c";

        final var alarmManager = new AlarmManager();
        Locator.setAlarmManager(alarmManager);

        alarmManager.raiseAlarmBySystemName(systemNameA, AlarmManager.Cause.systemNotRegistered);
        alarmManager.raiseAlarmBySystemId(systemIdB, AlarmManager.Cause.systemNotInDescription);
        alarmManager.raiseAlarmBySystemId(systemIdC, AlarmManager.Cause.systemNotInDescription);

        alarmManager.setAcknowledged(1, true);
        alarmManager.clearAlarmBySystemId(systemIdC, AlarmManager.Cause.systemNotInDescription);

        final var handler = new GetAllPdeAlarms();
        final HttpServiceRequest nameRequest = new MockRequest.Builder()
        .queryParameters(Map.of(
            "systemName", List.of(systemNameA)
        ))
        .build();
        final HttpServiceRequest ackRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "acknowledged", List.of("true")
            ))
            .build();
        final HttpServiceRequest severityRequest = new MockRequest.Builder()
            .queryParameters(Map.of(
                "severity", List.of("cleared")
            ))
            .build();

        HttpServiceResponse nameResponse = new MockResponse();
        HttpServiceResponse ackResponse = new MockResponse();
        HttpServiceResponse severityResponse = new MockResponse();

        try {
            handler.handle(nameRequest, nameResponse)
            .flatMap(nameResult -> {
                assertEquals(HttpStatus.OK, nameResponse.status().get());
                var alarms = (PdeAlarmList)nameResponse.body().get();
                assertEquals(1, alarms.count());
                assertEquals(systemNameA, alarms.data().get(0).systemName().get());
                return handler.handle(ackRequest, ackResponse);
            })
            .flatMap(ackResult -> {
                assertEquals(HttpStatus.OK, ackResponse.status().get());
                var alarms = (PdeAlarmList)ackResponse.body().get();
                assertEquals(1, alarms.count());
                assertEquals(systemIdB, alarms.data().get(0).systemId().get());
                return handler.handle(severityRequest, severityResponse);
            }).ifSuccess(severityResult -> {
                assertEquals(HttpStatus.OK, severityResponse.status().get());
                var alarms = (PdeAlarmList)severityResponse.body().get();
                assertEquals(1, alarms.count());
                assertEquals(systemIdC, alarms.data().get(0).systemId().get());
            })
            .onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void shouldPaginate() throws PdStoreException {

        final var alarmManager = new AlarmManager();
        Locator.setAlarmManager(alarmManager);

        for (int i = 0; i < 10; i++) {
            alarmManager.raiseAlarmBySystemId("System-" + i, AlarmManager.Cause.systemInactive);
        }

        final var handler = new GetAllPdeAlarms();
        final HttpServiceResponse response = new MockResponse();
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
                assertEquals(HttpStatus.OK, response.status().get());
                final var alarms = (PdeAlarmList)response.body().get();
                assertEquals(itemsPerPage, alarms.count());
                for (int i = 0; i < itemsPerPage; i++) {
                    int index = page * itemsPerPage + i;
                    assertEquals(index, alarms.data().get(i).id());
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
    public void shouldRejectNegativePage() throws PdStoreException {
        final var handler = new GetAllPdeAlarms();
        int page = -3;
        Locator.setAlarmManager(new AlarmManager());
        final HttpServiceResponse response = new MockResponse();
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
                String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                assertEquals(expectedErrorMessage, actualErrorMessage);
            }).onFailure(e -> {
                e.printStackTrace();
                assertNull(e);
            });
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

}