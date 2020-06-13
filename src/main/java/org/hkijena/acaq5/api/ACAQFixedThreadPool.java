package org.hkijena.acaq5.api;

import org.hkijena.acaq5.utils.InstantFuture;

import java.util.concurrent.*;

/**
 * Contains a fixed thread pool. Unfortunately, the one provided by {@link org.scijava.SciJava} is cached (unbounded)
 */
public class ACAQFixedThreadPool {
    private final int maxThreads;
    private ExecutorService executorService;

    /**
     * Initializes a pool with the specified amount of threads.
     * If maxThreads < 2, no {@link java.util.concurrent.ExecutorService} is created and all workloads are run in single-threaded mode
     * @param maxThreads number of threads. must be >= 1
     */
    public ACAQFixedThreadPool(int maxThreads)  {
        this.maxThreads = maxThreads;
        if(maxThreads <= 0)
            throw new IllegalArgumentException("Invalid number of threads: " + maxThreads);
        if(maxThreads > 1) {
            executorService = Executors.newFixedThreadPool(maxThreads);
        }
    }

    /**
     * Schedules a runnable
     * @param runnable the function
     * @return a future returning null if successful or an exception if there was an error
     */
    public Future<Exception> schedule(Runnable runnable) {
        if(executorService == null) {
            try {
                runnable.run();
                return new InstantFuture<>(null);
            }
            catch (Exception e) {
                return new InstantFuture<>(e);
            }
        }
        else {
            return executorService.submit(() -> {
                try {
                    runnable.run();
                    return null;
                }
                catch (Exception e) {
                    return e;
                }
            });
        }
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * Forces the thread pool to shut down
     */
    public void shutdown() {
        if(executorService != null) {
            executorService.shutdownNow();
        }
    }
}
