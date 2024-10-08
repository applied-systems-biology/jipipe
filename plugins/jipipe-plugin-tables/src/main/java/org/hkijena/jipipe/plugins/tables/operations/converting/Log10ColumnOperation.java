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

import org.hkijena.jipipe.plugins.tables.ConvertingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

/**
 * Applies a logarithm (base 10) function
 */
public class Log10ColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumnData apply(TableColumnData column) {
        double[] data = column.getDataAsDouble(column.getRows());
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.log10(data[i]);
        }
        return new DoubleArrayTableColumnData(data, column.getLabel());
    }
}
