package org.hkijena.acaq5.extensions.tables.operations.integrating;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.IntegratingColumnOperation;

/**
 * Implements calculating the standard deviation
 */
public class StatisticsStandardDeviationIntegratingColumnOperation implements IntegratingColumnOperation {

    private static final StandardDeviation standardDeviation = new StandardDeviation();

    @Override
    public TableColumn apply(TableColumn column) {
        double result = standardDeviation.evaluate(column.getDataAsDouble(column.getRows()));
        return new DoubleArrayTableColumn(new double[]{result}, column.getLabel());
    }
}
