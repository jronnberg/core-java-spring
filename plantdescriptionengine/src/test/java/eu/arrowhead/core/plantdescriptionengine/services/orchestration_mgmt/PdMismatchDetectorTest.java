package eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.PdMismatchDetector;
import eu.arrowhead.core.plantdescriptionengine.alarmmanager.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.orchestration_mgmt.rulebackingstore.RuleStoreException;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import eu.arrowhead.core.plantdescriptionengine.utils.MockSystemTracker;
import se.arkalix.net.http.client.HttpClient;

public class PdMismatchDetectorTest {

    @Test
    public void shouldReportSystemNotRegistered() throws SSLException, RuleStoreException, PdStoreException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var detector = new PdMismatchDetector(pdTracker);
        final var alarmManager = new AlarmManager();
        final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final var systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));

        Locator.setAlarmManager(alarmManager);
        Locator.setSystemTracker(systemTracker);
        detector.run();

        final String systemId = "System_A";
        final var system = new PdeSystemBuilder()
            .systemId(systemId)
            .systemName("System A")
            .ports(new ArrayList<>())
            .build();
        final var entry = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .systems(List.of(system))
            .connections(new ArrayList<>())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        pdTracker.put(entry);
        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);
        assertEquals(systemId, alarm.systemId().get());
        assertFalse(alarm.clearedAt().isPresent());
        assertEquals("warning", alarm.severity());
        assertEquals("System with ID '" + systemId +  "' cannot be found in the Service Registry.", alarm.description());
        assertEquals(false, alarm.acknowledged());
        assertFalse(alarm.clearedAt().isPresent());
    }

    @Test
    public void shouldClearSystemNotRegistered() throws SSLException, RuleStoreException, PdStoreException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var detector = new PdMismatchDetector(pdTracker);
        final var alarmManager = new AlarmManager();
        final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final var systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));

        Locator.setAlarmManager(alarmManager);
        Locator.setSystemTracker(systemTracker);
        detector.run();

        final String systemId = "System_A";
        final String systemName = "System A";
        final var system = new PdeSystemBuilder()
            .systemId(systemId)
            .systemName(systemName)
            .ports(new ArrayList<>())
            .build();
        final var entry = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .systems(List.of(system))
            .connections(new ArrayList<>())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        pdTracker.put(entry);

        final var srSystem = new SrSystemBuilder()
            .id(0)
            .systemName(systemName)
            .address("0.0.0.0")
            .port(5000)
            .authenticationInfo(null)
            .createdAt(Instant.now().toString())
            .updatedAt(Instant.now().toString())
            .build();

        systemTracker.addSystem(systemName, srSystem);
        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);
        assertEquals(1, alarms.size());
        assertEquals(systemId, alarm.systemId().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("warning", alarm.severity());
        assertEquals("System with ID '" + systemId +  "' cannot be found in the Service Registry.", alarm.description());
        assertEquals(false, alarm.acknowledged());
    }
}