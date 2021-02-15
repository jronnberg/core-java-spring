package eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt;

import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers.DeletePlantDescription;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers.GetAllPlantDescriptions;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import eu.arrowhead.core.plantdescriptionengine.providedservices.DtoReadExceptionCatcher;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers.AddPlantDescription;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers.ReplacePlantDescription;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers.GetPlantDescription;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.routehandlers.UpdatePlantDescription;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.dto.DtoReadException;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;

/**
 * This service enables management of Plant Descriptions in the Plant
 * Description Engine (PDE) core system.
 */
public class PdeManagementService {

    private final PlantDescriptionTracker pdTracker;
    private final boolean secure;

    /**
     * Class constructor.
     *
     * @param pdTracker An object that keeps track of Plant Description Entries.
     * @param secure    Indicates whether the service should run in secure mode.
     */
    public PdeManagementService(PlantDescriptionTracker pdTracker, boolean secure) {

        Objects.requireNonNull(pdTracker, "Expected AR System");
        Objects.requireNonNull(pdTracker, "Expected plant description map");

        this.pdTracker = pdTracker;
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
            .get("/pd/#id", new GetPlantDescription(pdTracker))
            .get("/pd", new GetAllPlantDescriptions(pdTracker))
            .post("/pd", new AddPlantDescription(pdTracker))
            .delete("/pd/#id", new DeletePlantDescription(pdTracker))
            .put("/pd/#id", new ReplacePlantDescription(pdTracker))
            .patch("/pd/#id", new UpdatePlantDescription(pdTracker))
            .catcher(DtoReadException.class, new DtoReadExceptionCatcher());

        if (secure) {
            service.accessPolicy(AccessPolicy.cloud());
        } else {
            service.accessPolicy(AccessPolicy.unrestricted());
        }
        return service;
    }

}