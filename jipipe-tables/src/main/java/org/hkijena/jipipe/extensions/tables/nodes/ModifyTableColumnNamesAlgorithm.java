package org.hkijena.jipipe.extensions.tables.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Rename & modify columns (expression)", description = "Uses an expression to modify/rename table column names. If a renaming yields an existing column, " +
        "it will be automatically renamed to be unique.")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
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
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ResultsTableData output = new ResultsTableData();
        output.addRows(input.getRowCount());
        ExpressionVariables variableSet = new ExpressionVariables();
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

        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @JIPipeDocumentation(name = "Renaming expression", description = "Determines how each column should be renamed. There is a variable 'value' available " +
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
