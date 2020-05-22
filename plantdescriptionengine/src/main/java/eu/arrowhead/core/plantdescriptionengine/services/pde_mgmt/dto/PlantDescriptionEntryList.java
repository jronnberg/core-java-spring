package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto;

import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.core.plantdescriptionengine.services.pde_monitor.MonitorInfo;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * Data Transfer Object (DTO) interface for lists of plant descriptions.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface PlantDescriptionEntryList {

    List<PlantDescriptionEntry> data();
    int count();

    /**
     * Returns a copy of a Plant Description Entry List supplemented with
     * monitor info.
     *
     * The resulting copy will contain inventory ID and system data, if
     * available.
     */
    // TODO: Remove this function
    static PlantDescriptionEntryListDto extend(PlantDescriptionEntryList entries, MonitorInfo monitorInfo) {
        List<PlantDescriptionEntryDto> data = new ArrayList<>();
        for (var entry : entries.data()) {
            data.add(PlantDescriptionEntry.extend(entry, monitorInfo));
        }
        return new PlantDescriptionEntryListBuilder()
            .count(entries.count())
            .data(data)
            .build();
    }
}
