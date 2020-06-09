package org.hkijena.acaq5.extensions.tables.operations.converting;

import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.ConvertingColumnOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Converts each entry into a factor
 */
public class FactorizeColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        Map<String, Integer> factors = new HashMap<>();
        for (int i = 0; i < column.getRows(); i++) {
            String v = column.getRowAsString(i);
            if(!factors.containsKey(v)) {
                factors.put(v, factors.size());
            }
        }
        double[] result = new double[column.getRows()];
        for (int i = 0; i < column.getRows(); i++) {
            String v = column.getRowAsString(i);
            result[i] = factors.get(v);
        }
        return new DoubleArrayTableColumn(result, column.getLabel());
    }
}
