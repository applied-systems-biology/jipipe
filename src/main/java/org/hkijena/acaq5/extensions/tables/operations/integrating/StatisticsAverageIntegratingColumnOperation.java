package org.hkijena.acaq5.extensions.tables.operations.integrating;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.IntegratingColumnOperation;

/**
 * Implements calculating the average
 */
public class StatisticsAverageIntegratingColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        double sum = 0;
        for (int i = 0; i < column.getRows(); i++) {
            sum += column.getRowAsDouble(i);
        }
        return new DoubleArrayTableColumn(new double[]{sum / column.getRows()}, column.getLabel());
    }
}
