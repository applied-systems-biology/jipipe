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

package org.hkijena.acaq5.extensions.tableoperations.ui.tableoperations;


import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableVectorOperation;

import java.util.Arrays;

/**
 * Counts all non-null and non-empty entries
 */
public class StatisticsCountNonNullVectorOperation implements ACAQTableVectorOperation {
    @Override
    public Object[] process(Object[] input) {
        long count = Arrays.stream(input).filter(o -> o != null && !("" + o).isEmpty()).count();
        return new Object[]{count};
    }

    @Override
    public boolean inputMatches(int inputItemCount) {
        return true;
    }

    @Override
    public int getOutputCount(int inputItemCount) {
        return 1;
    }
}
