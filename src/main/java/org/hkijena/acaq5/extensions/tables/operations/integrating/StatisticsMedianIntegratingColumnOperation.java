package org.hkijena.acaq5.extensions.tables.operations.integrating;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.IntegratingColumnOperation;

/**
 * Implements calculating the median value
 */
public class StatisticsMedianIntegratingColumnOperation implements IntegratingColumnOperation {

    private static final Median median = new Median();

    @Override
    public TableColumn apply(TableColumn column) {
        double medianResult = median.evaluate(column.getDataAsDouble(column.getRows()));
        return new DoubleArrayTableColumn(new double[]{medianResult}, column.getLabel());
    }
}
