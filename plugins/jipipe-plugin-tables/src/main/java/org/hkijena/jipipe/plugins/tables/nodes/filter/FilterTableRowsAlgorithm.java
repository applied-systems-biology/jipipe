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

package org.hkijena.jipipe.plugins.tables.nodes.filter;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Filter table rows", description = "Filters tables by iterating through each row and testing a filter expression.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class FilterTableRowsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public FilterTableRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FilterTableRowsAlgorithm(FilterTableRowsAlgorithm other) {
        super(other);
        this.filters = new JIPipeExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        List<Integer> selectedRows = new ArrayList<>();
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        variableSet.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variableSet);
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
        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Filters", description = "Allows you to select how to filter the values. " +
            "Each row is iterated individually and its columns are available as variables inside the expression. For example there are columns 'Area' and 'X'. " +
            "Then you can filter the table via an expression 'AREA > 100 AND X > 200 AND X < 1000'." +
            "Annotations are available as variables.")
    @JIPipeParameter("filters")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "<Columns>", description = "The column value of the current row")
    @JIPipeExpressionParameterVariable(name = "all.<Column>", description = "List of all values of the column")
    @JIPipeExpressionParameterVariable(name = "Row index", description = "The index of the table row. The first row is indexed with zero.", key = "index")
    @JIPipeExpressionParameterVariable(name = "Number of rows", description = "The number of rows.", key = "num_row")
    @JIPipeExpressionParameterVariable(name = "Number of columns", description = "The number of columns.", key = "num_cols")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public JIPipeExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(JIPipeExpressionParameter filters) {
        this.filters = filters;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
