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

package org.hkijena.jipipe.plugins.tables.operations.integrating;

import org.hkijena.jipipe.plugins.tables.SummarizingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

/**
 * Implements calculating the cumulative sum Y_0=X_0; Y_i=Y_{i-1}+X_i
 */
public class StatisticsCumulativeSumColumnOperation implements SummarizingColumnOperation {
    @Override
    public TableColumnData apply(TableColumnData column) {
        double sum = 0;
        for (int i = 0; i < column.getRows(); i++) {
            sum += sum + column.getRowAsDouble(i);
        }
        return new DoubleArrayTableColumnData(new double[]{sum}, column.getLabel());
    }
}
