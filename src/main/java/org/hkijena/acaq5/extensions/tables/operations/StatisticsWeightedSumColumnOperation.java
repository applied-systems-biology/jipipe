package org.hkijena.acaq5.extensions.tables.operations;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

/**
 * Implements calculating the weighted sum sum_{i=1,2,...}(X_i * i)
 */
public class StatisticsWeightedSumColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        double sum = 0;
        for (int i = 0; i < column.getRows(); i++) {
            sum += (i + 1) * column.getRowAsDouble(i);
        }
        return new DoubleArrayTableColumn(new double[] { sum }, column.getLabel());
    }
}
