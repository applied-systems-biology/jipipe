package org.hkijena.acaq5.extensions.tables.operations.converting;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.ConvertingColumnOperation;

import java.util.Arrays;

/**
 * Sorts the items ascending
 */
public class ToNumericColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        double[] data = column.getDataAsDouble(column.getRows());
        return new DoubleArrayTableColumn(data, column.getLabel());
    }
}
