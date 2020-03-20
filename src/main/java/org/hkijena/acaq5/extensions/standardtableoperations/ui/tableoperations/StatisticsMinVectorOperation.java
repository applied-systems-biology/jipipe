/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.extensions.standardtableoperations.ui.tableoperations;


import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableVectorOperation;

public class StatisticsMinVectorOperation implements ACAQTableVectorOperation {
    @Override
    public Object[] process(Object[] input) {
        double min = Double.MAX_VALUE;
        for (Object object : input) {
            if (object instanceof Number) {
                min = Math.min(min, ((Number) object).doubleValue());
            } else {
                min = Math.min(min, Double.parseDouble("" + object));
            }
        }
        return new Object[]{min};
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
