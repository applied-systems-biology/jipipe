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

/**
 * Applies a max(0, x) function
 */
public class ClampPositiveColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        double[] data = column.getDataAsDouble(column.getRows());
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.max(0, data[i]);
        }
        return new DoubleArrayTableColumn(data, column.getLabel());
    }
}
