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

package org.hkijena.jipipe.plugins.tables.nodes;

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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Rename & modify columns (expression)", description = "Uses an expression to modify/rename table column names. If a renaming yields an existing column, " +
        "it will be automatically renamed to be unique.")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Input", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
public class ModifyTableColumnNamesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression expression = new StringQueryExpression("value");

    public ModifyTableColumnNamesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ModifyTableColumnNamesAlgorithm(ModifyTableColumnNamesAlgorithm other) {
        super(other);
        this.expression = new StringQueryExpression(other.expression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ResultsTableData output = new ResultsTableData();
        output.addRows(input.getRowCount());
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        Set<String> existing = new HashSet<>();
        for (int col = 0; col < input.getColumnCount(); col++) {
            String name = input.getColumnName(col);
            variableSet.set("value", name);
            String result = StringUtils.nullToEmpty(expression.evaluate(variableSet));
            if (!result.isEmpty()) {
                String outputName = StringUtils.makeUniqueString(result, ".", existing);
                output.addColumn(outputName, input.getColumnReference(col), true);
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Renaming expression", description = "Determines how each column should be renamed. There is a variable 'value' available " +
            "that contains the current column. If you return an empty string, the column will be deleted.")
    @JIPipeParameter("expression")
    public StringQueryExpression getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(StringQueryExpression expression) {
        this.expression = expression;
    }

}
