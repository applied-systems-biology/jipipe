package org.hkijena.acaq5.extensions.tables.operations;

import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

/**
 * Implements calculating the kurtosis
 */
public class StatisticsKurtosisIntegratingColumnOperation implements IntegratingColumnOperation {

    private static final Kurtosis kurtosis = new Kurtosis();

    @Override
    public TableColumn run(TableColumn column) {
        double result = kurtosis.evaluate(column.getDataAsDouble(column.getRows()));
        return new DoubleArrayTableColumn(new double[] { result }, column.getLabel());
    }
}
