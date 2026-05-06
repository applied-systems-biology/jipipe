package org.hkijena.jipipe.api.ai;

public interface JIPipeAIModelRunner {
    JIPipeAIModelRunnerStatus getStatus();
    void start();
    void shutdown();

    default String getLastError() { return null; }
}
