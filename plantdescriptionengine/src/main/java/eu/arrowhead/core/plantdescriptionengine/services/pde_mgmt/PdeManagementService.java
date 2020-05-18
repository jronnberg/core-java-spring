package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import java.util.Objects;

import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandler.DescriptionDeleteHandler;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandler.DescriptionsGetHandler;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandler.DescriptionPostHandler;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandler.DescriptionPutHandler;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandler.DescriptionGetHandler;
import eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt.routehandler.DescriptionPatchHandler;
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
            .basePath("/pde")
            .get("/mgmt/pd/#id", new DescriptionGetHandler(entryMap))
            .get("/mgmt/pd", new DescriptionsGetHandler(entryMap))
            .post("/mgmt/pd", new DescriptionPostHandler(entryMap))
            .delete("/mgmt/pd/#id", new DescriptionDeleteHandler(entryMap))
            .put("/mgmt/pd/#id", new DescriptionPutHandler(entryMap))
            .patch("/mgmt/pd/#id", new DescriptionPatchHandler(entryMap));

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