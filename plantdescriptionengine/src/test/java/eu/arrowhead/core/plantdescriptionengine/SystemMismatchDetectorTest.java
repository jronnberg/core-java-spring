package eu.arrowhead.core.plantdescriptionengine;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.orchestratorclient.rulebackingstore.RuleStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystem;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.utils.MockSystemTracker;
import se.arkalix.net.http.client.HttpClient;

public class SystemMismatchDetectorTest {

    private PlantDescriptionTracker pdTracker;
    private HttpClient httpClient;
    private MockSystemTracker systemTracker;
    private AlarmManager alarmManager;
    private SystemMismatchDetector detector;

    private SrSystem getSrSystem(String systemName) {
        return new SrSystemBuilder()
        .id(0)
        .systemName(systemName)
        .address("0.0.0.0")
        .port(5000)
        .authenticationInfo(null)
        .createdAt(Instant.now().toString())
        .updatedAt(Instant.now().toString())
        .build();
    }

    private PdeSystemDto getSystem(String name, String id) {
        return new PdeSystemBuilder()
            .systemId(id)
            .systemName(name)
            .build();
    }

    private PlantDescriptionEntryDto getPdEntry(String systemName, boolean active) {
        final String systemId = "1234";
        final var system = getSystem(systemName, systemId);
        return new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(true)
            .systems(List.of(system))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private PlantDescriptionEntryDto getPdEntry(String systemName) {
        return getPdEntry(systemName, true);
    }

    @BeforeEach
    public void initEach() throws PdStoreException, SSLException {
        pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        httpClient = new HttpClient.Builder().insecure().build();
        systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));
        alarmManager = new AlarmManager();
        detector = new SystemMismatchDetector(pdTracker, systemTracker, alarmManager);
    }

    @Test
    public void shouldNotReportErrors() throws SSLException, RuleStoreException, PdStoreException {
        final String systemName = "System Z";

        pdTracker.put(getPdEntry(systemName));
        systemTracker.addSystem(getSrSystem(systemName));
        detector.run();

        final var alarms = alarmManager.getAlarms();
        assertEquals(0, alarms.size());
    }

    @Test
    public void shouldReportNotRegistered() throws SSLException, RuleStoreException, PdStoreException {
        detector.run();

        final String systemName = "System A";
        pdTracker.put(getPdEntry(systemName));
        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);
        assertEquals(systemName, alarm.systemName().get());
        assertFalse(alarm.clearedAt().isPresent());
        assertEquals("warning", alarm.severity());
        assertEquals(
            "System named '" + systemName +  "' cannot be found in the Service Registry.",
            alarm.description()
        );
        assertEquals(false, alarm.acknowledged());
        assertFalse(alarm.clearedAt().isPresent());
    }

    @Test
    public void shouldThrowException() throws SSLException, RuleStoreException, PdStoreException {
        detector.run();
        Exception exception = assertThrows(RuntimeException.class, () -> {
            // Create a PD with an unnamed system:
            pdTracker.put(getPdEntry(null));
        });
        assertEquals(
            "This version of the PDE cannot handle unnamed systems.",
            exception.getMessage()
        );
    }

    @Test
    public void shouldReportSystemNotInPd() throws SSLException, RuleStoreException, PdStoreException {

        final String systemNameA = "System A";
        final String systemNameB = "System B";

        // Create a plant description that only specifies System A.
        pdTracker.put(getPdEntry(systemNameA));
        systemTracker.addSystem(getSrSystem(systemNameA));
        systemTracker.addSystem(getSrSystem(systemNameB));

        detector.run();

        final var alarms = alarmManager.getAlarms();
        assertEquals(1, alarms.size());
        final var alarm = alarms.get(0);

        assertEquals(systemNameB, alarm.systemName().get());
        assertFalse(alarm.clearedAt().isPresent());
        assertEquals("warning", alarm.severity());
        assertEquals(
            "System named '" + systemNameB +  "' is not present in the active Plant Description.",
            alarm.description()
        );
        assertEquals(false, alarm.acknowledged());
        assertFalse(alarm.clearedAt().isPresent());
    }

    @Test
    public void shouldClearWhenSystemIsRegistered() throws SSLException, RuleStoreException, PdStoreException {

        final String systemNameA = "System A";
        final String systemNameB = "System B";

        final var systemA = getSystem(systemNameA, "a");
        final var systemB = getSystem(systemNameB, "b");

        final var pdeEntry = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(true)
            .systems(List.of(systemA, systemB))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        pdTracker.put(pdeEntry);
        systemTracker.addSystem(getSrSystem(systemNameA));

        // System B is missing, an alarm is created.
        detector.run();

        // System B is added, so the alarm should be cleared.
        systemTracker.addSystem(getSrSystem(systemNameB));

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertEquals(systemNameB, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals(
            "System named '" + systemNameB +  "' cannot be found in the Service Registry.",
            alarm.description()
        );
        assertEquals(false, alarm.acknowledged());
    }

    @Test
    public void shouldClearWhenPdIsRemoved() throws SSLException, RuleStoreException, PdStoreException {
        detector.run();

        final String systemName = "System C";
        final var entry = getPdEntry(systemName);
        pdTracker.put(entry);
        pdTracker.remove(entry.id());

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertEquals(systemName, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals(
            "System named '" + systemName +  "' cannot be found in the Service Registry.",
            alarm.description()
        );
        assertEquals(false, alarm.acknowledged());
    }

    @Test
    public void shouldClearWhenPdIsAdded() throws SSLException, RuleStoreException, PdStoreException {
        detector.run();

        final String systemName = "System D";

        systemTracker.addSystem(getSrSystem(systemName));
        pdTracker.put(getPdEntry(systemName));

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertEquals(systemName, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals(
            "System named '" + systemName +  "' is not present in the active Plant Description.",
            alarm.description()
        );
        assertEquals(false, alarm.acknowledged());
    }

    @Test
    public void shouldClearWhenSystemIsRemoved() throws SSLException, RuleStoreException, PdStoreException {
        detector.run();

        final String systemName = "System D";

        systemTracker.addSystem(getSrSystem(systemName));
        systemTracker.remove(systemName);

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertEquals(systemName, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals(
            "System named '" + systemName +  "' is not present in the active Plant Description.",
            alarm.description()
        );
        assertEquals(false, alarm.acknowledged());
    }

    @Test
    public void shouldClearWhenPdIsUpdated() throws SSLException, RuleStoreException, PdStoreException {

        final String systemNameA = "Sys-A";
        final String systemNameB = "Sys-B";

        final var entryWithOneSystem = getPdEntry(systemNameB);

        pdTracker.put(entryWithOneSystem);

        systemTracker.addSystem(getSrSystem(systemNameA));
        systemTracker.addSystem(getSrSystem(systemNameB));

        detector.run();

        final var entryWithTwoSystems = new PlantDescriptionEntryBuilder()
            .id(1)
            .plantDescription("Plant Description 1A")
            .active(true)
            .systems(List.of(getSystem(systemNameA, "a"), getSystem(systemNameB, "b")))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        pdTracker.put(entryWithTwoSystems);

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertEquals(systemNameA, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals(
            "System named '" + systemNameA +  "' is not present in the active Plant Description.",
            alarm.description()
        );
        assertEquals(false, alarm.acknowledged());
    }

}