package org.hkijena.acaq5.extensions.tables.operations;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Sorts the items ascending
 */
public class SortAscendingColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        if(column.isNumeric()) {
            double[] data = column.getDataAsDouble(column.getRows());
            Arrays.sort(data);
            return new DoubleArrayTableColumn(data, column.getLabel());
        }
        else {
            String[] data = column.getDataAsString(column.getRows());
            Arrays.sort(data);
            return new StringArrayTableColumn(data, column.getLabel());
        }
    }
}
