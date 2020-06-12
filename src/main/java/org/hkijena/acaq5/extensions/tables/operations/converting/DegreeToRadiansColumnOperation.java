package org.hkijena.acaq5.extensions.tables.operations.converting;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.ConvertingColumnOperation;

/**
 * Applies a abs(x) function
 */
public class DegreeToRadiansColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        double[] data = column.getDataAsDouble(column.getRows());
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] * Math.PI / 180.0;
        }
        return new DoubleArrayTableColumn(data, column.getLabel());
    }
}
