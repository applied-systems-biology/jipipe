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
import org.hkijena.jipipe.plugins.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

/**
 * Implements calculating the sum
 */
public class GetLastSummarizingColumnOperation implements SummarizingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        if (column.isNumeric()) {
            double value = column.getRows() > 0 ? column.getRowAsDouble(column.getRows() - 1) : 0;
            return new DoubleArrayTableColumn(new double[]{value}, column.getLabel());
        } else {
            String value = column.getRows() > 0 ? column.getRowAsString(column.getRows() - 1) : "";
            return new StringArrayTableColumn(new String[]{value}, column.getLabel());
        }
    }
}
