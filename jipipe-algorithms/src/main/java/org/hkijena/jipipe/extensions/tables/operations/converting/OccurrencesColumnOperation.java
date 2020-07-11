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

package org.hkijena.jipipe.extensions.tables.operations.converting;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.hkijena.jipipe.extensions.tables.ConvertingColumnOperation;
import org.hkijena.jipipe.extensions.tables.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.TableColumn;

/**
 * Converts each entry into a the number of occurrences within the column
 */
public class OccurrencesColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
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
        return new DoubleArrayTableColumn(result, column.getLabel());
    }
}
