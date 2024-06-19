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

package org.hkijena.jipipe.plugins.tables.nodes.columns;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

/**
 * Algorithm that removes columns
 */
@SetJIPipeDocumentation(name = "Remove table column", description = "Removes one or multiple columns by name")
@AddJIPipeNodeAlias(aliasName = "Filter table columns")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class RemoveColumnAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression filters = new StringQueryExpression();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public RemoveColumnAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RemoveColumnAlgorithm(RemoveColumnAlgorithm other) {
        super(other);
        this.filters = new StringQueryExpression(other.filters);
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = (ResultsTableData) iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        variablesMap.putCustomVariables(getDefaultCustomExpressionVariables());
        variablesMap.set("annotations", iterationStep.getMergedTextAnnotations());
        for (int col = 0; col < table.getColumnCount(); col++) {
            if (filters.test(table.getColumnName(col), variablesMap)) {
                table.removeColumnAt(col);
                --col;
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Filters", description = "Filter expression that is used to find columns to be removed. ")
    @JIPipeParameter("filters")
    @JIPipeExpressionParameterSettings(hint = "per column name (as value)")
    @JIPipeExpressionParameterVariable(key = "annotations", name = "Annotations map", description = "Map of text annotations")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public StringQueryExpression getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(StringQueryExpression filters) {
        this.filters = filters;
    }
}
