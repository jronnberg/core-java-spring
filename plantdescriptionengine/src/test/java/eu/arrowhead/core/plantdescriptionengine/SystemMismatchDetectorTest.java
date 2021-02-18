package eu.arrowhead.core.plantdescriptionengine;

import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystem;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.serviceregistry.dto.SrSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.InMemoryPdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PdeSystemDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.plantdescriptionengine.utils.MockSystemTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.arkalix.net.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SystemMismatchDetectorTest {

    private PlantDescriptionTracker pdTracker;
    private MockSystemTracker systemTracker;
    private AlarmManager alarmManager;
    private SystemMismatchDetector detector;

    private SrSystem getSrSystem(String systemName) {
        return new SrSystemBuilder().id(0).systemName(systemName).address("0.0.0.0").port(5000).authenticationInfo(null)
            .createdAt(Instant.now().toString()).updatedAt(Instant.now().toString()).build();
    }

    private PdeSystemDto getSystem(String name, String id) {
        return new PdeSystemBuilder().systemId(id).systemName(name).build();
    }

    private PlantDescriptionEntryDto getPdEntry(String systemName) {
        final String systemId = "1234";
        final var system = getSystem(systemName, systemId);
        return new PlantDescriptionEntryBuilder().id(1).plantDescription("Plant Description 1A").active(true)
            .systems(List.of(system)).createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    @BeforeEach
    public void initEach() throws PdStoreException, SSLException {
        pdTracker = new PlantDescriptionTracker(new InMemoryPdStore());
        HttpClient httpClient = new HttpClient.Builder().insecure().build();
        systemTracker = new MockSystemTracker(httpClient, new InetSocketAddress("0.0.0.0", 5000));
        alarmManager = new AlarmManager();
        detector = new SystemMismatchDetector(pdTracker, systemTracker, alarmManager);
    }

    @Test
    public void shouldNotReportErrors() throws PdStoreException {
        final String systemName = "System Z";

        pdTracker.put(getPdEntry(systemName));
        systemTracker.addSystem(getSrSystem(systemName));
        detector.run();

        final var alarms = alarmManager.getAlarms();
        assertEquals(0, alarms.size());
    }

    @Test
    public void shouldReportNotRegistered() throws PdStoreException {
        detector.run();

        final String systemName = "System A";
        pdTracker.put(getPdEntry(systemName));
        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);
        assertTrue(alarm.systemName().isPresent());
        assertEquals(systemName, alarm.systemName().get());
        assertFalse(alarm.clearedAt().isPresent());
        assertEquals("warning", alarm.severity());
        assertEquals("System named '" + systemName + "' cannot be found in the Service Registry.", alarm.description());
        assertFalse(alarm.acknowledged());
        assertFalse(alarm.clearedAt().isPresent());
    }

    @Test
    public void shouldReportSystemNotInPd() throws PdStoreException {

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

        assertTrue(alarm.systemName().isPresent());
        assertEquals(systemNameB, alarm.systemName().get());
        assertFalse(alarm.clearedAt().isPresent());
        assertEquals("warning", alarm.severity());
        assertEquals("System named '" + systemNameB + "' is not present in the active Plant Description.",
            alarm.description());
        assertFalse(alarm.acknowledged());
        assertFalse(alarm.clearedAt().isPresent());
    }

    @Test
    public void shouldClearWhenSystemIsRegistered() throws PdStoreException {

        final String systemNameA = "System A";
        final String systemNameB = "System B";

        final var systemA = getSystem(systemNameA, "a");
        final var systemB = getSystem(systemNameB, "b");

        final var pdeEntry = new PlantDescriptionEntryBuilder().id(1).plantDescription("Plant Description 1A")
            .active(true).systems(List.of(systemA, systemB)).createdAt(Instant.now()).updatedAt(Instant.now()).build();

        pdTracker.put(pdeEntry);
        systemTracker.addSystem(getSrSystem(systemNameA));

        // System B is missing, an alarm is created.
        detector.run();

        // System B is added, so the alarm should be cleared.
        systemTracker.addSystem(getSrSystem(systemNameB));

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertTrue(alarm.systemName().isPresent());
        assertEquals(systemNameB, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals("System named '" + systemNameB + "' cannot be found in the Service Registry.",
            alarm.description());
        assertFalse(alarm.acknowledged());
    }

    @Test
    public void shouldClearWhenPdIsRemoved() throws PdStoreException {
        detector.run();

        final String systemName = "System C";
        final var entry = getPdEntry(systemName);
        pdTracker.put(entry);
        pdTracker.remove(entry.id());

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertTrue(alarm.systemName().isPresent());
        assertEquals(systemName, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals("System named '" + systemName + "' cannot be found in the Service Registry.", alarm.description());
        assertFalse(alarm.acknowledged());
    }

    @Test
    public void shouldClearWhenPdIsAdded() throws PdStoreException {
        detector.run();

        final String systemName = "System D";

        systemTracker.addSystem(getSrSystem(systemName));
        pdTracker.put(getPdEntry(systemName));

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertTrue(alarm.systemName().isPresent());
        assertEquals(systemName, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals("System named '" + systemName + "' is not present in the active Plant Description.",
            alarm.description());
        assertFalse(alarm.acknowledged());
    }

    @Test
    public void shouldClearWhenSystemIsRemoved() {
        detector.run();

        final String systemName = "System D";

        systemTracker.addSystem(getSrSystem(systemName));
        systemTracker.remove(systemName);

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertTrue(alarm.systemName().isPresent());
        assertEquals(systemName, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals("System named '" + systemName + "' is not present in the active Plant Description.",
            alarm.description());
        assertFalse(alarm.acknowledged());
    }

    @Test
    public void shouldClearWhenPdIsUpdated() throws PdStoreException {

        final String systemNameA = "System A";
        final String systemNameB = "System B";

        final var entryWithOneSystem = getPdEntry(systemNameB);

        pdTracker.put(entryWithOneSystem);

        systemTracker.addSystem(getSrSystem(systemNameA));
        systemTracker.addSystem(getSrSystem(systemNameB));

        detector.run();

        final var entryWithTwoSystems = new PlantDescriptionEntryBuilder().id(1)
            .plantDescription("Plant Description 1A").active(true)
            .systems(List.of(getSystem(systemNameA, "a"), getSystem(systemNameB, "b"))).createdAt(Instant.now())
            .updatedAt(Instant.now()).build();

        pdTracker.put(entryWithTwoSystems);

        final var alarms = alarmManager.getAlarms();
        final var alarm = alarms.get(0);

        assertEquals(1, alarms.size());
        assertTrue(alarm.systemName().isPresent());
        assertEquals(systemNameA, alarm.systemName().get());
        assertTrue(alarm.clearedAt().isPresent());
        assertEquals("cleared", alarm.severity());
        assertEquals("System named '" + systemNameA + "' is not present in the active Plant Description.",
            alarm.description());
        assertFalse(alarm.acknowledged());
    }

}