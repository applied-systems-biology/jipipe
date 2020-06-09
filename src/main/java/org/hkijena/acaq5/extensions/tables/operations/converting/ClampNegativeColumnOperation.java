package org.hkijena.acaq5.extensions.tables.operations.converting;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.ConvertingColumnOperation;

/**
 * Applies a min(0, x) function
 */
public class ClampNegativeColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        double[] data = column.getDataAsDouble(column.getRows());
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.min(0, data[i]);
        }
        return new DoubleArrayTableColumn(data, column.getLabel());
    }
}
