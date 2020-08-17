package eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt;

import eu.arrowhead.core.plantdescriptionengine.services.service_registry_mgmt.dto.SrSystem;

public interface SystemUpdateListener {
    void onSystemAdded(SrSystem system);
    void onSystemRemoved(SrSystem system);
}