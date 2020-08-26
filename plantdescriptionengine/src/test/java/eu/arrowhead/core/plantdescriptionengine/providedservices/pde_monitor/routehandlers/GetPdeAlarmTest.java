package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarm;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class GetPdeAlarmTest {

    @Test
    public void shouldRetrieveAlarm() {

        final var alarmManager = new AlarmManager();
        Locator.setAlarmManager(alarmManager);

        alarmManager.raiseSystemNotInDescription("System A");
        final var alarm = alarmManager.getAlarms().get(0);

        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(alarm.id())))
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new GetPdeAlarm();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.OK, response.status().get());
                    final var retrievedAlarm = (PdeAlarm)response.body().get();
                    assertEquals(alarm.systemName().get(), retrievedAlarm.systemName().get());
                    assertEquals(alarm.severity(), retrievedAlarm.severity());
                    assertEquals(alarm.acknowledged(), retrievedAlarm.acknowledged());
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
    public void shouldRejectInvalidId() {

        Locator.setAlarmManager(new AlarmManager());
        final String invalidEntryId = "Invalid ID";
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(invalidEntryId)))
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new GetPdeAlarm();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "'" + invalidEntryId + "' is not a valid PDE Alarm ID.";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
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
    public void shouldRejectNonexistentId() {

        Locator.setAlarmManager(new AlarmManager());
        final String nonexistentId = "31";
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(nonexistentId)))
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new GetPdeAlarm();

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.status().get());
                    String expectedErrorMessage = "PDE Alarm with ID '" + nonexistentId + "' not found.";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                }).onFailure(e -> {
                    e.printStackTrace();
                    assertNull(e);
                });
            } catch (final Exception e) {
                e.printStackTrace();
                assertNull(e);
            }
    }
}