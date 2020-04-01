/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.tableanalyzer;

/**
 * Operation that processes a vector
 */
public interface ACAQTableVectorOperation {
    /**
     * @param input input objects
     * @return output objects
     */
    Object[] process(Object[] input);

    /**
     * Returns if the input can be processed
     *
     * @param inputItemCount how many items the input contains
     * @return if the input can be processed
     */
    boolean inputMatches(int inputItemCount);

    /**
     * @param inputItemCount how many items the input contains
     * @return how many entries are generated
     */
    int getOutputCount(int inputItemCount);
}
