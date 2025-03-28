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

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
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
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.HashSet;
import java.util.Set;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Filter tables", description = "Filters tables by their properties.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class FilterTablesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter("");

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public FilterTablesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FilterTablesAlgorithm(FilterTablesAlgorithm other) {
        super(other);
        this.filters = new JIPipeExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap(iterationStep);

        parameters.set("num_rows", input.getRowCount());
        parameters.set("num_cols", input.getColumnCount());
        parameters.set("column_names", input.getColumnNames());

        if (filters.test(parameters)) {
            iterationStep.addOutputData(getFirstOutputSlot(), input, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Filters", description = "Expression that is applied per table to determine if it is filtered out. Must return a boolean.")
    @JIPipeParameter("filters")
    @AddJIPipeExpressionParameterVariable(fromClass = VariablesInfo.class)
    public JIPipeExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(JIPipeExpressionParameter filters) {
        this.filters = filters;
    }

    public static class VariablesInfo implements JIPipeExpressionVariablesInfo {

        public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_rows", "Number of rows", "The number of rows in the table"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_cols", "Number of columns", "The number of columns in the table"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("column_names", "Column names", "Names of the table columns"));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
