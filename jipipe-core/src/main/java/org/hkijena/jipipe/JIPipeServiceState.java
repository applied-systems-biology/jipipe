package org.hkijena.jipipe;

/**
 * The states that the JIPipe service can be at
 */
public enum JIPipeServiceState {
    Uninitialized,
    Initializing,
    Initialized,
    ShuttingDown,
    Shutdown,
}
