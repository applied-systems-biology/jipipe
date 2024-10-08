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

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.plugins.tables.ConvertingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

/**
 * Sorts the items ascending
 */
public class ToNumericColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumnData apply(TableColumnData column) {
        if (column.isNumeric())
            return column;
        double[] data = new double[column.getRows()];
        for (int row = 0; row < column.getRows(); row++) {
            data[row] = NumberUtils.createDouble(column.getRowAsString(row));
        }
        return new DoubleArrayTableColumnData(data, column.getLabel());
    }
}
