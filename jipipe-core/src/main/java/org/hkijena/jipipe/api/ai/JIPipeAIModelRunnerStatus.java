package org.hkijena.jipipe.api.ai;

public enum JIPipeAIModelRunnerStatus {
    Unloaded,
    Loading,
    Idle,
    Busy,
    Unloading,
    Failed
}
