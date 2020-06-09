package org.hkijena.acaq5.extensions.tables.operations;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

/**
 * Implements calculating the min value
 */
public class StatisticsMinIntegratingColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < column.getRows(); i++) {
            min = Math.min(column.getRowAsDouble(i), min);
        }
        return new DoubleArrayTableColumn(new double[] { min }, column.getLabel());
    }
}
