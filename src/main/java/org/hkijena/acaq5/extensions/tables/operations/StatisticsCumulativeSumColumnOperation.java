package org.hkijena.acaq5.extensions.tables.operations;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

/**
 * Implements calculating the cumulative sum Y_0=X_0; Y_i=Y_{i-1}+X_i
 */
public class StatisticsCumulativeSumColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        double sum = 0;
        for (int i = 0; i < column.getRows(); i++) {
            sum += sum + column.getRowAsDouble(i);
        }
        return new DoubleArrayTableColumn(new double[] { sum }, column.getLabel());
    }
}
