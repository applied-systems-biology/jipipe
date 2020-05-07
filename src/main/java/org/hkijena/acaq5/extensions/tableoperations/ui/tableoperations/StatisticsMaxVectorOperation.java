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

/**
 * Finds the maximum of entries
 */
public class StatisticsMaxVectorOperation implements ACAQTableVectorOperation {
    @Override
    public Object[] process(Object[] input) {
        double max = Double.MAX_VALUE;
        for (Object object : input) {
            if (object instanceof Number) {
                max = Math.max(max, ((Number) object).doubleValue());
            } else {
                max = Math.max(max, Double.parseDouble("" + object));
            }
        }
        return new Object[]{max};
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
