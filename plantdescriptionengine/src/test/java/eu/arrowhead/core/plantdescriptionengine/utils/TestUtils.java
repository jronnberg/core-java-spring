package eu.arrowhead.core.plantdescriptionengine.utils;

import java.time.Instant;
import java.util.ArrayList;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionDto;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.dto.PlantDescriptionEntryDto;

public class TestUtils {

    public static PlantDescriptionEntryDto createEntry(float id) {
        final Instant now = Instant.now();
        return new PlantDescriptionEntryBuilder()
            .id(id)
            .plantDescription("Plant Description 1A")
            .active(false)
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    public static PlantDescriptionDto createDescription() {
        return new PlantDescriptionBuilder()
            .plantDescription("Plant Description 1A")
            .active(true)
            .include(new ArrayList<>())
            .systems(new ArrayList<>())
            .connections(new ArrayList<>())
            .build();
    }

}