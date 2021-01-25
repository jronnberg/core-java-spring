package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockServiceResponse;
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

        alarmManager.raiseSystemNotInDescription("System A");
        final var alarm = alarmManager.getAlarms().get(0);

        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(alarm.id())))
            .build();
        final HttpServiceResponse response = new MockServiceResponse();
        final var handler = new GetPdeAlarm(alarmManager);

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.OK, response.status().get());
                    final var retrievedAlarm = (PdeAlarm)response.body().get();
                    assertEquals(alarm.systemName().get(), retrievedAlarm.systemName().get());
                    assertEquals(alarm.severity(), retrievedAlarm.severity());
                    assertEquals(alarm.acknowledged(), retrievedAlarm.acknowledged());
                }).onFailure(e -> {
                    assertNull(e);
                });
            } catch (final Exception e) {
                assertNull(e);
            }
    }

    @Test
    public void shouldRejectInvalidId() {

        final String invalidEntryId = "Invalid ID";
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(invalidEntryId)))
            .build();
        final HttpServiceResponse response = new MockServiceResponse();
        final var handler = new GetPdeAlarm(new AlarmManager());

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.status().get());
                    String expectedErrorMessage = "'" + invalidEntryId + "' is not a valid PDE Alarm ID.";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                }).onFailure(e -> {
                    assertNull(e);
                });
            } catch (final Exception e) {
                assertNull(e);
            }
    }

    @Test
    public void shouldRejectNonexistentId() {

        final String nonexistentId = "31";
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(nonexistentId)))
            .build();
        final HttpServiceResponse response = new MockServiceResponse();
        final var handler = new GetPdeAlarm(new AlarmManager());

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.status().get());
                    String expectedErrorMessage = "PDE Alarm with ID '" + nonexistentId + "' not found.";
                    String actualErrorMessage = ((ErrorMessage)response.body().get()).error();
                    assertEquals(expectedErrorMessage, actualErrorMessage);
                }).onFailure(e -> {
                    assertNull(e);
                });
            } catch (final Exception e) {
                assertNull(e);
            }
    }
}