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

package org.hkijena.acaq5.extensions.tables.operations.integrating;

import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.hkijena.acaq5.extensions.tables.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.TableColumn;
import org.hkijena.acaq5.extensions.tables.IntegratingColumnOperation;

/**
 * Implements calculating the kurtosis
 */
public class StatisticsKurtosisIntegratingColumnOperation implements IntegratingColumnOperation {

    private static final Kurtosis kurtosis = new Kurtosis();

    @Override
    public TableColumn apply(TableColumn column) {
        double result = kurtosis.evaluate(column.getDataAsDouble(column.getRows()));
        return new DoubleArrayTableColumn(new double[]{result}, column.getLabel());
    }
}
