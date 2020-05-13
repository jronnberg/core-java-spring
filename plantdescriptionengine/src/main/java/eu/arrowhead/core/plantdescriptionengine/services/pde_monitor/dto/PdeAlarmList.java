package eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.dto;

import java.util.ArrayList;
import java.util.List;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for lists of PDE alarms.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface PdeAlarmList {

    List<PdeAlarm> data();
    int count();

    default String asString() {
        String result = "PdeAlarmList[count=" + count() + ",data=[";
        List<String> strings = new ArrayList<>();
        for (var alarm : data()) {
            strings.add(String.valueOf(alarm.id()));
        }
        result += String.join(",", strings);
        result += "]]";
        return result;
    }
}
