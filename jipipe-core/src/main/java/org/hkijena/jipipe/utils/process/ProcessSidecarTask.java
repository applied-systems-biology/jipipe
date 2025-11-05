package org.hkijena.jipipe.utils.process;

/**
 * An additional task attached to a running process
 */
public interface ProcessSidecarTask {
    void start(ExtendedExecutor executor) throws Exception;

    void stop();
}
