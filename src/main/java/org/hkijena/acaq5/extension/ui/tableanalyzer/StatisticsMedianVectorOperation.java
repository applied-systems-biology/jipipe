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

package org.hkijena.acaq5.extension.ui.tableanalyzer;


import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableVectorOperation;

import java.util.Arrays;

public class StatisticsMedianVectorOperation implements ACAQTableVectorOperation {
    @Override
    public Object[] process(Object[] input) {
        double[] numbers = new double[input.length];
        for (int i = 0; i < input.length; ++i) {
            if (input[i] instanceof Number) {
                numbers[i] = ((Number) input[i]).doubleValue();
            } else {
                numbers[i] = Double.parseDouble("" + input[i]);
            }
        }
        Arrays.sort(numbers);
        if (numbers.length % 2 == 0) {
            double floor = numbers[numbers.length / 2 - 1];
            double ceil = numbers[numbers.length / 2];
            return new Object[]{(floor + ceil) / 2.0};
        } else {
            return new Object[]{numbers[numbers.length / 2]};
        }
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
