package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator;

import java.net.InetSocketAddress;

import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.RuleStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.PlantDescriptionTracker;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.util.concurrent.Future;

public class MockOrchestratorClient extends OrchestratorClient {

	public MockOrchestratorClient(
        HttpClient httpClient,
        RuleStore ruleStore,
        PlantDescriptionTracker pdTracker,
        InetSocketAddress orchestratorAddress
    ) {
		super(httpClient, ruleStore, pdTracker, orchestratorAddress);
	}

    @Override
    public Future<Void> initialize() {
        return Future.done();
    }
    
}
