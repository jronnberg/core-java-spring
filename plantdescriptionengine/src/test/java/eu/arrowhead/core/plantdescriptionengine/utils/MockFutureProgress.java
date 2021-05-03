package eu.arrowhead.core.plantdescriptionengine.utils;

import se.arkalix.util.Result;
import se.arkalix.util.concurrent.Future;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Mock MockFutureProgress implementation used for testing.
 */
public class MockFutureProgress<R> {

    private final R value;

    MockFutureProgress(final R value) {
        this.value = value;
    }

    // TODO: Remove me!

}