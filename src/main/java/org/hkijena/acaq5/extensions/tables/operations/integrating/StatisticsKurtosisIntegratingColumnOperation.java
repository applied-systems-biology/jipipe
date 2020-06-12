package org.hkijena.acaq5.extensions.tables.operations.integrating;

import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.IntegratingColumnOperation;

/**
 * Implements calculating the kurtosis
 */
public class StatisticsKurtosisIntegratingColumnOperation implements IntegratingColumnOperation {

    private static final Kurtosis kurtosis = new Kurtosis();

    @Override
    public TableColumn apply(TableColumn column) {
        double result = kurtosis.evaluate(column.getDataAsDouble(column.getRows()));
        return new DoubleArrayTableColumn(new double[] { result }, column.getLabel());
    }
}
