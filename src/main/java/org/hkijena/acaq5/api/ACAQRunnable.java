package org.hkijena.acaq5.api;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ACAQRunnable {
    void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled);
}
