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

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.hkijena.jipipe.plugins.tables.SummarizingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

/**
 * Implements calculating the standard deviation
 */
public class StatisticsStandardDeviationSummarizingColumnOperation implements SummarizingColumnOperation {

    private static final StandardDeviation standardDeviation = new StandardDeviation();

    @Override
    public TableColumnData apply(TableColumnData column) {
        double result = standardDeviation.evaluate(column.getDataAsDouble(column.getRows()));
        return new DoubleArrayTableColumnData(new double[]{result}, column.getLabel());
    }
}
