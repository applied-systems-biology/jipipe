package org.hkijena.jipipe.api.service;

/**
 * The states that the JIPipe service can be at
 */
public enum JIPipeServiceState {
    Uninitialized,
    Initializing,
    Initialized,
    Error,
    ShuttingDown,
    Shutdown,
}
