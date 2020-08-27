package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmUpdateBuilder;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class UpdatePdeAlarmTest {

    @Test
    public void shouldAcknowledgeAlarm() {

        final String systemNameA = "System A";

        final var alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotInDescription(systemNameA);
        final var alarm = alarmManager.getAlarms().get(0);
        assertFalse(alarm.acknowledged());

        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(alarm.id())))
            .body(new PdeAlarmUpdateBuilder()
                .acknowledged(true)
                .build())
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new UpdatePdeAlarm(alarmManager);

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.OK, response.status().get());
                    final var updatedAlarm = (PdeAlarm)response.body().get();
                    assertTrue(updatedAlarm.acknowledged());
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

        final String invalidEntryId = "Invalid ID";
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(invalidEntryId)))
            .body(new PdeAlarmUpdateBuilder()
                .acknowledged(true)
                .build())
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new UpdatePdeAlarm(new AlarmManager());

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

        final String nonexistentId = "31";
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(nonexistentId)))
            .body(new PdeAlarmUpdateBuilder()
                .acknowledged(true)
                .build())
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new UpdatePdeAlarm(new AlarmManager());

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

    @Test
    public void shouldNotChangeAlarm() {

        final var alarmManager = new AlarmManager();

        alarmManager.raiseSystemNotInDescription("SystemA");
        final var alarm = alarmManager.getAlarms().get(0);
        assertFalse(alarm.acknowledged());

        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(alarm.id())))
            .body(new PdeAlarmUpdateBuilder()
                .build())
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new UpdatePdeAlarm(alarmManager);

        try {
            handler.handle(request, response)
                .ifSuccess(result -> {
                    assertEquals(HttpStatus.OK, response.status().get());
                    final var updatedAlarm = (PdeAlarm)response.body().get();
                    assertFalse(updatedAlarm.acknowledged());
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