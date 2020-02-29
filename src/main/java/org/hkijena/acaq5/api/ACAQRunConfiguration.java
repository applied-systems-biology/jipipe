package org.hkijena.acaq5.api;

import java.nio.file.Path;

public interface ACAQRunConfiguration {
    /**
     * Returns the output path where results are stored
     *
     * @return
     */
    Path getOutputPath();

    /**
     * If true, data will be written to disk when its not used anymore
     *
     * @return
     */
    boolean isFlushingEnabled();

    /**
     * If not null, the run is only executed up to the algorithm with given Id
     *
     * @return
     */
    String getEndAlgorithmId();

    /**
     * If true, only the end algorithm will run
     *
     * @return
     */
    boolean isOnlyRunningEndAlgorithm();

    /**
     * If true, data is only saved instead of flushed (set to null)
     *
     * @return
     */
    boolean isFlushingKeepsDataEnabled();
}
