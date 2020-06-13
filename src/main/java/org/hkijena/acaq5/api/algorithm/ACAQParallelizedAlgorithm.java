package org.hkijena.acaq5.api.algorithm;

/**
 * Algorithm that supports parallelization
 */
public interface ACAQParallelizedAlgorithm {

    /**
     * User-defined parameter to control if parallelization is enabled
     * @param parallelizationEnabled if parallelization is enabled
     */
    void setParallelizationEnabled(boolean parallelizationEnabled);

    /**
     * @return if parallelization is enabled by user
     */
    boolean isParallelizationEnabled();

    /**
     * Indicates to the algorithm base implementation if parallelization is supported.
     * Use this function to indicate to the algorithm to conditionally enable/disable parallelization.
     * @return If the algorithm supports automated parallelization.
     */
    boolean supportsParallelization();

    /**
     * Returns how many threads the actual algorithm requires.
     * Based on this value, the base algorithm creates batches.
     * @return number of threads used by the workload
     */
    int getThreadsPerBatch();
}
