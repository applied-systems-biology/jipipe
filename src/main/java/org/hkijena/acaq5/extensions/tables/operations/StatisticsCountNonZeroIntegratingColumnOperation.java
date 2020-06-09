package org.hkijena.acaq5.extensions.tables.operations;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

/**
 * Implements counting non-zero elements
 */
public class StatisticsCountNonZeroIntegratingColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        double sum = 0;
        for (int i = 0; i < column.getRows(); i++) {
            sum += column.getRowAsDouble(i) != 0 ? 1 : 0;
        }
        return new DoubleArrayTableColumn(new double[] { sum }, column.getLabel());
    }
}
