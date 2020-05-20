package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.DeletePlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.GetAllPlantDescriptions;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.AddPlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.ReplacePlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.GetPlantDescription;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandlers.UpdatePlantDescription;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;

public class PdeManagementService {

    private final PlantDescriptionEntryMap entryMap;

    /**
     * Constructor of a PdeManagementService.
     *
     * @param entryMap An object that maps ID:s to Plant Description
     *                            Entries.
     */
    public PdeManagementService(PlantDescriptionEntryMap entryMap) {
        Objects.requireNonNull(entryMap, "Expected plant description map");
        this.entryMap = entryMap;
    }

    /**
     * @return A HTTP Service that handles requests for retrieving and updating
     *         Plant Description data.
     *
     * @param secure Indicates whether the returned service should run in secure
     *               mode.
     */
    public HttpService getService(boolean secure) {
        HttpService service = new HttpService()
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

    /**
     * @return A HTTP Service that handles requests for retrieving and updating
     *         Plant Description data, running in secure mode.
     */
    public HttpService getService() {
        return getService(true);
    }

}