/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.api.algorithm;

/**
 * Algorithm that supports parallelization
 */
public interface ACAQParallelizedAlgorithm {

    /**
     * @return if parallelization is enabled by user
     */
    boolean isParallelizationEnabled();

    /**
     * User-defined parameter to control if parallelization is enabled
     *
     * @param parallelizationEnabled if parallelization is enabled
     */
    void setParallelizationEnabled(boolean parallelizationEnabled);

    /**
     * Indicates to the algorithm base implementation if parallelization is supported.
     * Use this function to indicate to the algorithm to conditionally enable/disable parallelization.
     *
     * @return If the algorithm supports automated parallelization.
     */
    boolean supportsParallelization();

    /**
     * Returns how many threads the actual algorithm requires.
     * Based on this value, the base algorithm creates batches.
     *
     * @return number of threads used by the workload
     */
    int getParallelizationBatchSize();
}
