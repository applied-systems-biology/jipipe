package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.parameters.ACAQParameterHolder;

import java.nio.file.Path;

/**
 * Configures an {@link ACAQRun}
 */
public interface ACAQRunConfiguration extends ACAQParameterHolder {
    /**
     * Returns the output path where results are stored
     *
     * @return the output path where results are stored
     */
    Path getOutputPath();

    /**
     * If true, data will be written to disk when its not used anymore
     *
     * @return If true, data will be written to disk when its not used anymore
     */
    boolean isFlushingEnabled();

    /**
     * If not null, the run is only executed up to the algorithm with given Id
     *
     * @return If not null, the run is only executed up to the algorithm with given Id
     */
    String getEndAlgorithmId();

    /**
     * If true, only the end algorithm will run
     *
     * @return If true, only the end algorithm will run
     */
    boolean isOnlyRunningEndAlgorithm();

    /**
     * If true, data is only saved instead of flushed (set to null)
     *
     * @return If true, data is only saved instead of flushed (set to null)
     */
    boolean isFlushingKeepsDataEnabled();
}
