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

package org.hkijena.acaq5.extensions.tables.operations.converting;

import org.hkijena.acaq5.extensions.tables.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.TableColumn;
import org.hkijena.acaq5.extensions.tables.ConvertingColumnOperation;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts each entry into a factor
 */
public class FactorizeColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        Map<String, Integer> factors = new HashMap<>();
        for (int i = 0; i < column.getRows(); i++) {
            String v = column.getRowAsString(i);
            if (!factors.containsKey(v)) {
                factors.put(v, factors.size());
            }
        }
        double[] result = new double[column.getRows()];
        for (int i = 0; i < column.getRows(); i++) {
            String v = column.getRowAsString(i);
            result[i] = factors.get(v);
        }
        return new DoubleArrayTableColumn(result, column.getLabel());
    }
}
