package org.hkijena.acaq5.extensions.tables.operations.integrating;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.IntegratingColumnOperation;

/**
 * Implements calculating the weighted sum sum_{i=1,2,...}(X_i * i)
 */
public class StatisticsWeightedSumColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        double sum = 0;
        for (int i = 0; i < column.getRows(); i++) {
            sum += (i + 1) * column.getRowAsDouble(i);
        }
        return new DoubleArrayTableColumn(new double[] { sum }, column.getLabel());
    }
}
