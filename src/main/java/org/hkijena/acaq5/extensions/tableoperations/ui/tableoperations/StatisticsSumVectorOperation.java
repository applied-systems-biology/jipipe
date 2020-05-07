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
 * Calculates the sum
 */
public class StatisticsSumVectorOperation implements ACAQTableVectorOperation {
    @Override
    public Object[] process(Object[] input) {
        double sum = 0;
        for (Object object : input) {
            if (object instanceof Number) {
                sum += ((Number) object).doubleValue();
            } else {
                sum += Double.parseDouble("" + object);
            }
        }
        return new Object[]{sum};
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
