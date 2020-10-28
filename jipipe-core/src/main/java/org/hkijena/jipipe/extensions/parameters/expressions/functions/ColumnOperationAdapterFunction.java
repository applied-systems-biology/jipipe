/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.tables.ColumnOperation;
import org.hkijena.jipipe.extensions.tables.IntegratingColumnOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.util.List;

/**
 * An adapter from {@link org.hkijena.jipipe.extensions.tables.ColumnOperation} to {@link org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction}.
 * This adapter determines the number of arguments based on the ColumnOperation type:
 * {@link org.hkijena.jipipe.extensions.tables.IntegratingColumnOperation} will have infinite inputs
 * All other column operations will have one parameter. Please note that the operation should only create exactly one output.
 */
public class ColumnOperationAdapterFunction extends ExpressionFunction {
    private final ColumnOperation columnOperation;

    public ColumnOperationAdapterFunction(ColumnOperation columnOperation, String name) {
        super(name, 1, columnOperation instanceof IntegratingColumnOperation ? Integer.MAX_VALUE : 1);
        this.columnOperation = columnOperation;
    }

    public ColumnOperation getColumnOperation() {
        return columnOperation;
    }

    @Override
    public Object evaluate(List<Object> parameters) {
        boolean isNumeric = parameters.stream().allMatch(o -> o instanceof Number);
        if(isNumeric) {
            double[] arr = new double[parameters.size()];
            for (int i = 0; i < parameters.size(); i++) {
                arr[i] = ((Number)parameters.get(i)).doubleValue();
            }
            TableColumn result = columnOperation.apply(new DoubleArrayTableColumn(arr, "x"));
            if(result.getRows() > 1)
                throw new UnsupportedOperationException("Function returned more than one row!");
            return result.getRowAsDouble(0);
        }
        else {
            String[] arr = new String[parameters.size()];
            for (int i = 0; i < parameters.size(); i++) {
                arr[i] = "" + parameters.get(i);
             }
            TableColumn result = columnOperation.apply(new StringArrayTableColumn(arr, "x"));
            if(result.getRows() > 1)
                throw new UnsupportedOperationException("Function returned more than one row!");
            return result.getRowAsString(0);
        }
    }
}
