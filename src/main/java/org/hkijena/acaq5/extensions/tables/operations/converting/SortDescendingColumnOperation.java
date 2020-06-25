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

import org.apache.commons.lang3.ArrayUtils;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.ConvertingColumnOperation;

import java.util.Arrays;
import java.util.Collections;

/**
 * Sorts the items ascending
 */
public class SortDescendingColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        if (column.isNumeric()) {
            double[] data = column.getDataAsDouble(column.getRows());
            Arrays.sort(data);
            ArrayUtils.reverse(data);
            return new DoubleArrayTableColumn(data, column.getLabel());
        } else {
            String[] data = column.getDataAsString(column.getRows());
            Arrays.sort(data, Collections.reverseOrder());
            return new StringArrayTableColumn(data, column.getLabel());
        }
    }
}
