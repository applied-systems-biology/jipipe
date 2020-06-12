package org.hkijena.acaq5.extensions.tables.operations.converting;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.ConvertingColumnOperation;

/**
 * Sets NaN values to zero
 */
public class RemoveNaNColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        double[] values = column.getDataAsDouble(column.getRows());
        for (int i = 0; i < values.length; i++) {
            if (Double.isNaN(values[i])) {
                values[i] = 0;
            }
        }
        return new DoubleArrayTableColumn(values, column.getLabel());
    }
}
