package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import java.util.function.Consumer;

import se.arkalix.util.Result;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.FutureProgress;

/**
 * Mock MockFutureProgress implementation used for testing.
 */
public class MockFutureProgress<R> implements FutureProgress<R> {

    private final R value;

    MockFutureProgress(R value) {
        this.value = value;
    }

    @Override
    public void onResult(Consumer<Result<R>> consumer) {
        consumer.accept(Result.success(value));
    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
    }

    @Override
    public Future<R> addProgressListener(Listener listener) {
        return null;
    }

}