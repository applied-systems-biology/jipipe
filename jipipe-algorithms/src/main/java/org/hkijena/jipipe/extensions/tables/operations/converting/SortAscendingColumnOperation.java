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

import org.hkijena.jipipe.extensions.tables.ConvertingColumnOperation;
import org.hkijena.jipipe.extensions.tables.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.TableColumn;

import java.util.Arrays;

/**
 * Sorts the items ascending
 */
public class SortAscendingColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        if (column.isNumeric()) {
            double[] data = column.getDataAsDouble(column.getRows());
            Arrays.sort(data);
            return new DoubleArrayTableColumn(data, column.getLabel());
        } else {
            String[] data = column.getDataAsString(column.getRows());
            Arrays.sort(data);
            return new StringArrayTableColumn(data, column.getLabel());
        }
    }
}
