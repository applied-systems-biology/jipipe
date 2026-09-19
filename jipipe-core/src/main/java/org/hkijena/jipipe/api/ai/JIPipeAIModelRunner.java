package org.hkijena.jipipe.api.ai;

import org.hkijena.jipipe.api.microservice.MicroserviceState;

public interface JIPipeAIModelRunner {
    MicroserviceState getStatus();
    void start();
    void shutdown();
    String getModelId();
    default String getLastError() { return null; }
}
