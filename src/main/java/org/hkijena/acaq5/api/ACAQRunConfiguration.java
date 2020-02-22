package org.hkijena.acaq5.api;

import java.nio.file.Path;

public interface ACAQRunConfiguration {
    /**
     * Returns the output path where results are stored
     * @return
     */
    Path getOutputPath();

    /**
     * If true, data will be written to disk when its not used anymore
     * @return
     */
    boolean isFlushingEnabled();

    /**
     * If not null, the run is only executed up to the algorithm with the given id
     * @return
     */
    String getEndAlgorithmId();
}
