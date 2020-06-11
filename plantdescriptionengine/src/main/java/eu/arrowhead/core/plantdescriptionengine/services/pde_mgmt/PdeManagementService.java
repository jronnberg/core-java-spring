package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.DeletePlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.GetAllPlantDescriptions;
import eu.arrowhead.core.plantdescriptionengine.pdentrymap.PlantDescriptionEntryMap;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.AddPlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.ReplacePlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.GetPlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.UpdatePlantDescription;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;

public class PdeManagementService {

    private final PlantDescriptionEntryMap entryMap;
    private final boolean secure;

    /**
     * Class constructor.
     *
     * @param entryMap An object that maps ID:s to Plant Description Entries.
     * @param secure   Indicates whether the service should run in secure mode.
     */
    public PdeManagementService(PlantDescriptionEntryMap entryMap, boolean secure) {

        Objects.requireNonNull(entryMap, "Expected AR System");
        Objects.requireNonNull(entryMap, "Expected plant description map");

        this.entryMap = entryMap;
        this.secure = secure;
    }

    /**
     * Registers this service with an Arrowhead system, eventually making it
     * accessible to remote Arrowhead systems.
     *
     * @param arSystem An Arrowhead Framework system used to provide this
     *                 service.
     * @return A HTTP Service used to manage Plant Descriptions.
     */
    public HttpService getService() {
        var service = new HttpService()
            .name("pde-mgmt")
            .encodings(EncodingDescriptor.JSON)
            .basePath("/pde/mgmt")
            .get("/pd/#id", new GetPlantDescription(entryMap))
            .get("/pd", new GetAllPlantDescriptions(entryMap))
            .post("/pd", new AddPlantDescription(entryMap))
            .delete("/pd/#id", new DeletePlantDescription(entryMap))
            .put("/pd/#id", new ReplacePlantDescription(entryMap))
            .patch("/pd/#id", new UpdatePlantDescription(entryMap));

        if (secure) {
            service.accessPolicy(AccessPolicy.cloud());
        } else {
            service.accessPolicy(AccessPolicy.unrestricted());
        }
        return service;
    }

}