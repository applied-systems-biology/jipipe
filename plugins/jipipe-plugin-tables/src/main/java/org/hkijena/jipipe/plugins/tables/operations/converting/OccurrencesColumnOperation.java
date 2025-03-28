/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.tables.operations.converting;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.hkijena.jipipe.plugins.tables.ConvertingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

/**
 * Converts each entry into a the number of occurrences within the column
 */
public class OccurrencesColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumnData apply(TableColumnData column) {
        Multiset<String> factors = HashMultiset.create();
        for (int i = 0; i < column.getRows(); i++) {
            String v = column.getRowAsString(i);
            factors.add(v);
        }
        double[] result = new double[column.getRows()];
        for (int i = 0; i < column.getRows(); i++) {
            String v = column.getRowAsString(i);
            result[i] = factors.count(v);
        }
        return new DoubleArrayTableColumnData(result, column.getLabel());
    }
}
