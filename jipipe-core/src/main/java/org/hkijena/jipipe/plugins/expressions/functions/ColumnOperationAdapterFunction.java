/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.expressions.functions;

import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;
import org.hkijena.jipipe.plugins.tables.ColumnOperation;
import org.hkijena.jipipe.plugins.tables.SummarizingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.StringArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An adapter from {@link org.hkijena.jipipe.plugins.tables.ColumnOperation} to {@link org.hkijena.jipipe.plugins.expressions.ExpressionFunction}.
 * This adapter determines the number of arguments based on the ColumnOperation type:
 * {@link SummarizingColumnOperation} will have infinite inputs
 * All other column operations will have one parameter. Please note that the operation should only create exactly one output.
 */
public class ColumnOperationAdapterFunction extends ExpressionFunction {
    private final ColumnOperation columnOperation;

    public ColumnOperationAdapterFunction(ColumnOperation columnOperation, String name) {
        super(name, 1, columnOperation instanceof SummarizingColumnOperation ? Integer.MAX_VALUE : 1);
        this.columnOperation = columnOperation;
    }

    public ColumnOperation getColumnOperation() {
        return columnOperation;
    }

    /**
     * Returns info about the parameter at index
     *
     * @param index the parameter index
     * @return the info
     */
    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("x" + (index + 1), "Can be a string, number, or collection. Collections are expanded into the parameters (flatten).", String.class, Number.class, Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        if (parameters.size() == 1) {
            Object o = parameters.get(0);
            if (o instanceof List) {
                parameters = (List<Object>) o;
            }
        }
        if (parameters.stream().anyMatch(o -> o instanceof Collection)) {
            // Requires parameter expansion
            List<Object> unExpanded = parameters;
            parameters = new ArrayList<>();
            for (Object item : unExpanded) {
                if (item instanceof Collection) {
                    parameters.addAll((Collection) item);
                } else {
                    parameters.add(item);
                }
            }
        }

        boolean isNumeric = parameters.stream().allMatch(o -> o instanceof Number);
        if (isNumeric) {
            double[] arr = new double[parameters.size()];
            for (int i = 0; i < parameters.size(); i++) {
                arr[i] = ((Number) parameters.get(i)).doubleValue();
            }
            TableColumnData result = columnOperation.apply(new DoubleArrayTableColumnData(arr, "x"));
            if (result.getRows() > 1)
                throw new UnsupportedOperationException("Function returned more than one row!");
            return result.getRowAsDouble(0);
        } else {
            String[] arr = new String[parameters.size()];
            for (int i = 0; i < parameters.size(); i++) {
                arr[i] = "" + parameters.get(i);
            }
            TableColumnData result = columnOperation.apply(new StringArrayTableColumnData(arr, "x"));
            if (result.getRows() > 1)
                throw new UnsupportedOperationException("Function returned more than one row!");
            return result.getRowAsString(0);
        }
    }
}
