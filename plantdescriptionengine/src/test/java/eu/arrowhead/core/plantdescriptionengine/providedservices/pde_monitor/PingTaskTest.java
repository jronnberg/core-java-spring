package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor;

import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.utils.MockClientResponse;
import eu.arrowhead.core.plantdescriptionengine.utils.RequestMatcher;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.arkalix.description.ServiceDescription;
import se.arkalix.description.SystemDescription;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.descriptor.TransportDescriptor;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.query.ServiceQuery;
import se.arkalix.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PingTaskTest {

    @Test
    public void shouldClearSystemInactive() {

        final String serviceUri = "http://some-service-uri";
        final String systemName = "System-xyz";

        final HttpClient httpClient = Mockito.mock(HttpClient.class);
        final AlarmManager alarmManager = new AlarmManager();
        final ServiceQuery serviceQuery = Mockito.mock(ServiceQuery.class);
        final ServiceDescription service = Mockito.mock(ServiceDescription.class);
        final Set<ServiceDescription> services = Set.of(service);
        final Future<Set<ServiceDescription>> resolveResult = Future.success(services);
        final MockClientResponse response = new MockClientResponse();
        response.status(HttpStatus.OK);
        final SystemDescription provider = Mockito.mock(SystemDescription.class);
        final InetSocketAddress address = new InetSocketAddress("1.1.1.1", 8443);
        when(serviceQuery.name("monitorable")).thenReturn(serviceQuery);
        when(serviceQuery.transports(TransportDescriptor.HTTP)).thenReturn(serviceQuery);
        when(serviceQuery.encodings(EncodingDescriptor.JSON)).thenReturn(serviceQuery);
        when(service.provider()).thenReturn(provider);
        when(service.uri()).thenReturn(serviceUri);
        when(provider.name()).thenReturn(systemName);
        when(provider.socketAddress()).thenReturn(address);

        final HttpClientRequest expectedRequest = new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri(service.uri() + "/ping")
            .header("accept", "application/json");

        when(httpClient.send(any(InetSocketAddress.class), argThat(new RequestMatcher(expectedRequest))))
            .thenReturn(Future.success(response));
        when(serviceQuery.resolveAll()).thenReturn(resolveResult);

        final PingTask pingTask = new PingTask(serviceQuery, httpClient, alarmManager);

        alarmManager.raiseSystemInactive(systemName);
        assertFalse(alarmManager.getAlarms()
            .get(0)
            .clearedAt()
            .isPresent());
        pingTask.run();
        assertTrue(alarmManager.getAlarms()
            .get(0)
            .clearedAt()
            .isPresent());
    }

    @Test
    public void shouldNotClearAlarmOnError() {

        final String systemName = "System-xyz";

        final HttpClient httpClient = Mockito.mock(HttpClient.class);
        final AlarmManager alarmManager = new AlarmManager();
        final ServiceQuery serviceQuery = Mockito.mock(ServiceQuery.class);
        final Throwable error = new Throwable("Some error");
        final Future<Set<ServiceDescription>> resolveResult = Future.failure(error);
        final MockClientResponse response = new MockClientResponse();
        response.status(HttpStatus.OK);

        when(serviceQuery.resolveAll()).thenReturn(resolveResult);

        final PingTask pingTask = new PingTask(serviceQuery, httpClient, alarmManager);

        alarmManager.raiseSystemInactive(systemName);
        assertFalse(alarmManager.getAlarms()
            .get(0)
            .clearedAt()
            .isPresent());
        pingTask.run();
        assertFalse(alarmManager.getAlarms()
            .get(0)
            .clearedAt()
            .isPresent());
    }

    @Test
    public void shouldRaiseSystemInactive() {

        final String serviceUri = "http://some-service-uri";
        final String systemName = "System-xyz";
        final InetSocketAddress address = new InetSocketAddress("1.1.1.1", 8443);

        final HttpClient httpClient = Mockito.mock(HttpClient.class);
        final AlarmManager alarmManager = new AlarmManager();
        final ServiceQuery serviceQuery = Mockito.mock(ServiceQuery.class);
        final ServiceDescription service = Mockito.mock(ServiceDescription.class);
        final Set<ServiceDescription> services = Set.of(service);
        final Future<Set<ServiceDescription>> resolveResult = Future.success(services);
        final MockClientResponse response = new MockClientResponse();
        response.status(HttpStatus.OK);
        final SystemDescription provider = Mockito.mock(SystemDescription.class);
        when(serviceQuery.name("monitorable")).thenReturn(serviceQuery);
        when(serviceQuery.transports(TransportDescriptor.HTTP)).thenReturn(serviceQuery);
        when(serviceQuery.encodings(EncodingDescriptor.JSON)).thenReturn(serviceQuery);
        when(service.provider()).thenReturn(provider);
        when(service.uri()).thenReturn(serviceUri);
        when(provider.name()).thenReturn(systemName);
        when(provider.socketAddress()).thenReturn(address);
        final Throwable error = new Throwable("Some error");

        final HttpClientRequest expectedRequest = new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri(service.uri() + "/ping")
            .header("accept", "application/json");
        when(httpClient.send(eq(address), argThat(new RequestMatcher(expectedRequest))))
            .thenReturn(Future.failure(error));
        when(serviceQuery.resolveAll()).thenReturn(resolveResult);

        final PingTask pingTask = new PingTask(serviceQuery, httpClient, alarmManager);

        assertEquals(0, alarmManager.getAlarms().size());
        pingTask.run();
        assertEquals(1, alarmManager.getAlarms().size());
    }

}
