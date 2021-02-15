package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.routehandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmManager;
import eu.arrowhead.core.plantdescriptionengine.alarms.AlarmSeverity;
import eu.arrowhead.core.plantdescriptionengine.providedservices.dto.ErrorMessage;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.BooleanParameter;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.IntParameter;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.ParseError;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.QueryParamParser;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.QueryParameter;
import eu.arrowhead.core.plantdescriptionengine.providedservices.requestvalidation.StringParameter;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarm;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmDto;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_monitor.dto.PdeAlarmListBuilder;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpRouteHandler;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.net.http.service.HttpServiceResponse;
import se.arkalix.util.concurrent.Future;

/**
 * Handles HTTP requests to retrieve PDE alarms.
 */
public class GetAllPdeAlarms implements HttpRouteHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetAllPdeAlarms.class);

    private final AlarmManager alarmManager;

    /**
     * Constructor.
     *
     * @param alarmManager Object used for managing PDE alarms.
     */
    public GetAllPdeAlarms(AlarmManager alarmManager) {
        Objects.requireNonNull(alarmManager, "Expected Alarm Manager.");
        this.alarmManager = alarmManager;
    }

    /**
     * Handles an HTTP call to acquire a list of PDE alarms raised by the PDE.
     *
     * @param request  HTTP request object.
     * @param response HTTP response containing an alarm list.
     */
    @Override
    public Future<HttpServiceResponse> handle(
        final HttpServiceRequest request,
        final HttpServiceResponse response
    ) throws Exception {

        final List<QueryParameter> requiredParameters = null;
        List<String> severityValues = new ArrayList<>();
        for (var severity : AlarmSeverity.values()) {
            severityValues.add(severity.toString());
        }
        severityValues.add("not_cleared");

        final List<QueryParameter> acceptedParameters = List.of(
            new IntParameter("page")
                .min(0)
                .requires(new IntParameter("item_per_page")
                    .min(0)),
            new StringParameter("sort_field")
                .legalValues(List.of("id", "raisedAt", "updatedAt")),
            new StringParameter("direction")
                .legalValues(List.of("ASC", "DESC"))
                .setDefault("ASC"),
            new StringParameter("systemName"),
            new StringParameter("severity")
                .legalValues(severityValues),
            new BooleanParameter("acknowledged")
        );

        QueryParamParser parser;

        try {
            parser = new QueryParamParser(requiredParameters, acceptedParameters, request);
        } catch (ParseError error) {
            logger.error("Encountered the following error(s) while parsing an HTTP request: " +
                error.getMessage());
            return Future.success(response
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorMessage.of(error.getMessage()))
            );
        }

        List<PdeAlarmDto> alarms = alarmManager.getAlarms();

        final Optional<String> sortField = parser.getString("sort_field");
        if (sortField.isPresent()) {
            final String sortDirection = parser.getString("direction").get();
            final boolean sortAscending = (sortDirection.equals("ASC") ? true : false);
            PdeAlarm.sort(alarms, sortField.get(), sortAscending);
        }

        final Optional<Integer> page = parser.getInt("page");
        if (page.isPresent()) {
            int itemsPerPage = parser.getInt("item_per_page").get();

            int from = Math.min(page.get() * itemsPerPage, alarms.size());
            int to = Math.min(from + itemsPerPage, alarms.size());

            alarms = alarms.subList(from, to);
        }

        final Optional<String> systemName = parser.getString("systemName");
        if (systemName.isPresent()) {
            PdeAlarm.filterBySystemName(alarms, systemName.get());
        }

        final Optional<String> severityValue = parser.getString("severity");
        if (severityValue.isPresent()) {
            PdeAlarm.filterBySeverity(alarms, severityValue.get());
        }

        final Optional<Boolean> acknowledged = parser.getBoolean("acknowledged");
        if (acknowledged.isPresent()) {
            PdeAlarm.filterAcknowledged(alarms, acknowledged.get());
        }

        response.body(new PdeAlarmListBuilder()
            .data(alarms)
            .count(alarms.size())
            .build());
        response.status(HttpStatus.OK);
        return Future.success(response);
    }
}