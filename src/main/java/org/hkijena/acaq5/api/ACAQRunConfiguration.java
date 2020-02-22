package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;

import java.nio.file.Path;
import java.util.Set;

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
     * If not null, the run is only executed up to the algorithm
     * @return
     */
    ACAQAlgorithm getEndAlgorithm();

    /**
     * If not null or empty, restrict only to specific samples
     * @return
     */
    Set<String> getSampleRestrictions();
}
