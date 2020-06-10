package org.hkijena.acaq5.extensions.tables.operations.integrating;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.IntegratingColumnOperation;

/**
 * Implements calculating the max value
 */
public class StatisticsMaxIntegratingColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < column.getRows(); i++) {
            max = Math.min(column.getRowAsDouble(i), max);
        }
        return new DoubleArrayTableColumn(new double[]{max}, column.getLabel());
    }
}
