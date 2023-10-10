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
 *
 */

package org.hkijena.jipipe.extensions.tables.nodes.filter;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.CustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Filter table rows", description = "Filters tables by iterating through each row and testing a filter expression.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class FilterTableRowsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customExpressionVariables;
    private DefaultExpressionParameter filters = new DefaultExpressionParameter();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public FilterTableRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FilterTableRowsAlgorithm(FilterTableRowsAlgorithm other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.filters = new DefaultExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        List<Integer> selectedRows = new ArrayList<>();
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.putAnnotations(dataBatch.getMergedTextAnnotations());
        customExpressionVariables.writeToVariables(variableSet, true, "custom.", true, "custom");
        variableSet.set("num_rows", input.getRowCount());
        variableSet.set("num_cols", input.getColumnCount());
        for (int col = 0; col < input.getColumnCount(); col++) {
            TableColumn column = input.getColumnReference(col);
            if (column.isNumeric()) {
                variableSet.set("all." + column.getLabel(), Doubles.asList(column.getDataAsDouble(column.getRows())));
            } else {
                variableSet.set("all." + column.getLabel(), Arrays.asList(column.getDataAsString(column.getRows())));
            }
        }
        for (int row = 0; row < input.getRowCount(); row++) {
            if (progressInfo.isCancelled())
                return;
            for (int col = 0; col < input.getColumnCount(); col++) {
                variableSet.set(input.getColumnName(col), input.getValueAt(row, col));
            }
            variableSet.set("index", row);
            if (filters.test(variableSet)) {
                selectedRows.add(row);
            }
        }

        ResultsTableData output = input.getRows(selectedRows);
        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @JIPipeDocumentation(name = "Filters", description = "Allows you to select how to filter the values. " +
            "Each row is iterated individually and its columns are available as variables inside the expression. For example there are columns 'Area' and 'X'. " +
            "Then you can filter the table via an expression 'AREA > 100 AND X > 200 AND X < 1000'." +
            "Annotations are available as variables.")
    @JIPipeParameter("filters")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "<Columns>", description = "The column value of the current row")
    @ExpressionParameterSettingsVariable(name = "all.<Column>", description = "List of all values of the column")
    @ExpressionParameterSettingsVariable(name = "Row index", description = "The index of the table row. The first row is indexed with zero.", key = "index")
    @ExpressionParameterSettingsVariable(name = "Number of rows", description = "The number of rows.", key = "num_row")
    @ExpressionParameterSettingsVariable(name = "Number of columns", description = "The number of columns.", key = "num_cols")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public DefaultExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(DefaultExpressionParameter filters) {
        this.filters = filters;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-expression-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }
}
