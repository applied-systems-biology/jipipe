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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.HashSet;
import java.util.Set;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Filter tables", description = "Filters tables by their properties.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class FilterTablesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter filters = new DefaultExpressionParameter("");
    private boolean includeAnnotations = true;

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
        this.includeAnnotations = other.includeAnnotations;
        this.filters = new DefaultExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

        ExpressionVariables parameters = new ExpressionVariables();
        if (includeAnnotations) {
            for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
                parameters.set(annotation.getName(), annotation.getValue());
            }
        }
        parameters.set("num_rows", input.getRowCount());
        parameters.set("num_cols", input.getColumnCount());
        parameters.set("column_names", input.getColumnNames());

        if (filters.test(parameters)) {
            dataBatch.addOutputData(getFirstOutputSlot(), input, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Include annotations", description = "If enabled, annotations are also available as string variables. Please note that " +
            "table data specific variables will overwrite annotations with the same name.")
    @JIPipeParameter("include-annotations")
    public boolean isIncludeAnnotations() {
        return includeAnnotations;
    }

    @JIPipeParameter("include-annotations")
    public void setIncludeAnnotations(boolean includeAnnotations) {
        this.includeAnnotations = includeAnnotations;
    }

    @JIPipeDocumentation(name = "Filters", description = "Expression that is applied per table to determine if it is filtered out. Must return a boolean.")
    @JIPipeParameter("filters")
    @ExpressionParameterSettingsVariable(fromClass = VariableSource.class)
    public DefaultExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(DefaultExpressionParameter filters) {
        this.filters = filters;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Number of rows", "The number of rows in the table", "num_rows"));
            VARIABLES.add(new ExpressionParameterVariable("Number of columns", "The number of columns in the table", "num_cols"));
            VARIABLES.add(new ExpressionParameterVariable("Column names", "Names of the table columns", "column_names"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
