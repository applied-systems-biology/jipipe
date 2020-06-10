package org.hkijena.acaq5.extensions.tables.operations.converting;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.ConvertingColumnOperation;

/**
 * Converts each entry into a the number of occurrences within the column
 */
public class OccurrencesColumnOperation implements ConvertingColumnOperation {
    @Override
    public TableColumn run(TableColumn column) {
        Multiset<String> factors = HashMultiset.create();
        for (int i = 0; i < column.getRows(); i++) {
            String v = column.getRowAsString(i);
            factors.add(v);
        }
        double[] result = new double[column.getRows()];
        for (int i = 0; i < column.getRows(); i++) {
            String v = column.getRowAsString(i);
            result[i] = factors.count(v);
        }
        return new DoubleArrayTableColumn(result, column.getLabel());
    }
}
