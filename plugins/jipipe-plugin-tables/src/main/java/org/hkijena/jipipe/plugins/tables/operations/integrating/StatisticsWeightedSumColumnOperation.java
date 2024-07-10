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
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

/**
 * Implements calculating the weighted sum sum_{i=1,2,...}(X_i * i)
 */
public class StatisticsWeightedSumColumnOperation implements SummarizingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        double sum = 0;
        for (int i = 0; i < column.getRows(); i++) {
            sum += (i + 1) * column.getRowAsDouble(i);
        }
        return new DoubleArrayTableColumn(new double[]{sum}, column.getLabel());
    }
}
