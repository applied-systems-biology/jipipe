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

import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;
import org.hkijena.jipipe.plugins.tables.SummarizingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

/**
 * Implements calculating the geometric mean
 */
public class StatisticsGeometricMeanSummarizingColumnOperation implements SummarizingColumnOperation {

    private static final GeometricMean geometricMean = new GeometricMean();

    @Override
    public TableColumnData apply(TableColumnData column) {
        double result = geometricMean.evaluate(column.getDataAsDouble(column.getRows()));
        return new DoubleArrayTableColumnData(new double[]{result}, column.getLabel());
    }
}
