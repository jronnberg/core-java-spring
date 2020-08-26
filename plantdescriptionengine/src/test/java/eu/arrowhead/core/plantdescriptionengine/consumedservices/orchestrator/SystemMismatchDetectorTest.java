package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.arrowhead.core.plantdescriptionengine.SystemMismatchDetector;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.RuleStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.utils.Locator;
import eu.arrowhead.core.plantdescriptionengine.utils.MockSystemTracker;
import se.arkalix.net.http.client.HttpClient;

public class SystemMismatchDetectorTest {

    @Test
    public void shouldReportSystemNotRegistered() throws SSLException, RuleStoreException, PdStoreException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var detector = new SystemMismatchDetector(pdTracker);
        final var alarmManager = new AlarmManager();
        final HttpClient httpClient = new HttpClient.Builder().insecure().build();
        final var systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));

        Locator.setAlarmManager(alarmManager);
        Locator.setSystemTracker(systemTracker);
        detector.run();

        final String systemName = "System A";
        final var system = new PdeSystemBuilder()
            .systemId("system_a")
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
        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);
        assertEquals(systemName, alarm.systemName().get());
        assertFalse(alarm.clearedAt().isPresent());
        assertEquals("warning", alarm.severity());
        assertEquals("System named '" + systemName +  "' cannot be found in the Service Registry.", alarm.description());
        assertEquals(false, alarm.acknowledged());
        assertFalse(alarm.clearedAt().isPresent());
    }

    @Test
    public void shouldClearSystemNotRegistered() throws SSLException, RuleStoreException, PdStoreException {
        final var pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        final var detector = new SystemMismatchDetector(pdTracker);
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
        assertEquals(systemName, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals("System named '" + systemName +  "' cannot be found in the Service Registry.", alarm.description());
        assertEquals(false, alarm.acknowledged());
    }
}