package org.hkijena.jipipe.api.ai;

public interface JIPipeAiModel {
    JIPipeAiModelStatus getStatus();
    void start();
    void shutdown();
}
