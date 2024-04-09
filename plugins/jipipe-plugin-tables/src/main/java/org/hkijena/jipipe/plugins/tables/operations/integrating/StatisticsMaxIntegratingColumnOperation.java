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

import org.hkijena.jipipe.plugins.tables.IntegratingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

/**
 * Implements calculating the max value
 */
public class StatisticsMaxIntegratingColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < column.getRows(); i++) {
            max = Math.max(column.getRowAsDouble(i), max);
        }
        return new DoubleArrayTableColumn(new double[]{max}, column.getLabel());
    }
}
