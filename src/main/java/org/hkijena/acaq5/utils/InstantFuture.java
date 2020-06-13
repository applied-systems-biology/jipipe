package org.hkijena.acaq5.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A dummy {@link Future}
 *
 * @param <T> return type
 */
public class InstantFuture<T> implements Future<T> {

    private final T returnValue;

    /**
     * Initializes a new instance
     *
     * @param returnValue the return value
     */
    public InstantFuture(T returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return returnValue;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return returnValue;
    }
}
