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

package org.hkijena.jipipe.extensions.tables.operations.integrating;

import org.hkijena.jipipe.extensions.tables.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.IntegratingColumnOperation;
import org.hkijena.jipipe.extensions.tables.TableColumn;

/**
 * Implements calculating the min value
 */
public class StatisticsMinIntegratingColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < column.getRows(); i++) {
            min = Math.min(column.getRowAsDouble(i), min);
        }
        return new DoubleArrayTableColumn(new double[]{min}, column.getLabel());
    }
}
