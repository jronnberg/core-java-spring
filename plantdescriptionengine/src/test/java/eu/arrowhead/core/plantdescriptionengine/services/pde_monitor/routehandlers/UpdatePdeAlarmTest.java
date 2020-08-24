package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.routehandlers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import eu.arrowhead.core.plantdescriptionengine.utils.MockRequest;
import eu.arrowhead.core.plantdescriptionengine.utils.MockResponse;
import eu.arrowhead.core.plantdescriptionengine.alarmmanager.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto.PdeAlarmUpdateBuilder;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;

public class UpdatePdeAlarmTest {

    @Test
    public void shouldUpdateAlarm() {

        final String systemNameA = "System A";

        final var alarmManager = new AlarmManager();
        Locator.setAlarmManager(alarmManager);

        alarmManager.raiseAlarmBySystemName(systemNameA, AlarmManager.Cause.systemNotRegistered);
        final var alarm = alarmManager.getAlarms().get(0);
        assertFalse(alarm.acknowledged());

        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(alarm.id())))
            .body(new PdeAlarmUpdateBuilder()
                .acknowledged(true)
                .build())
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new UpdatePdeAlarm();

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

        Locator.setAlarmManager(new AlarmManager());
        final String invalidEntryId = "Invalid ID";
        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(invalidEntryId)))
            .body(new PdeAlarmUpdateBuilder()
                .acknowledged(true)
                .build())
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new UpdatePdeAlarm();

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
            .body(new PdeAlarmUpdateBuilder()
                .acknowledged(true)
                .build())
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new UpdatePdeAlarm();

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

        final String systemNameA = "System A";

        final var alarmManager = new AlarmManager();
        Locator.setAlarmManager(alarmManager);

        alarmManager.raiseAlarmBySystemName(systemNameA, AlarmManager.Cause.systemNotRegistered);
        final var alarm = alarmManager.getAlarms().get(0);
        assertFalse(alarm.acknowledged());

        final HttpServiceRequest request = new MockRequest.Builder()
            .pathParameters(List.of(String.valueOf(alarm.id())))
            .body(new PdeAlarmUpdateBuilder()
                .build())
            .build();
        final HttpServiceResponse response = new MockResponse();
        final var handler = new UpdatePdeAlarm();

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