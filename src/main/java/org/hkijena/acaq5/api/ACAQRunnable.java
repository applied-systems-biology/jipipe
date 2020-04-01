package org.hkijena.acaq5.api;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runnable that can be scheduled, canceled, and reports progress
 */
public interface ACAQRunnable {
    /**
     * Runs the runnable
     *
     * @param onProgress  Function that consumes progress reports
     * @param isCancelled Function that supplies if the runnable should be canceled
     */
    void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled);
}
