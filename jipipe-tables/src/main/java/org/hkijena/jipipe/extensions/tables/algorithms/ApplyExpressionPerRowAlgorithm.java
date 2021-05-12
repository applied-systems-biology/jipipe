package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Apply expression per row", description = "Applies an expression for each row. The column values are available as variables.")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ApplyExpressionPerRowAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ExpressionTableColumnGeneratorProcessorParameterList expressionList = new ExpressionTableColumnGeneratorProcessorParameterList();

    public ApplyExpressionPerRowAlgorithm(JIPipeNodeInfo info) {
        super(info);
        expressionList.addNewInstance();
    }

    public ApplyExpressionPerRowAlgorithm(ApplyExpressionPerRowAlgorithm other) {
        super(other);
        this.expressionList = new ExpressionTableColumnGeneratorProcessorParameterList(other.expressionList);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData data = (ResultsTableData) dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate();
        ExpressionParameters variableSet = new ExpressionParameters();
        variableSet.set("num_rows", data.getRowCount());
        for (ExpressionTableColumnGeneratorProcessor expression : expressionList) {
            List<Object> generatedValues = new ArrayList<>();
            variableSet.set("num_cols", data.getColumnCount());
            variableSet.set("column", data.getColumnIndex(expression.getValue()));
            variableSet.set("column_name", expression.getValue());
            for (int row = 0; row < data.getRowCount(); row++) {
                variableSet.set("row", row);
                for (int col = 0; col < data.getColumnCount(); col++) {
                    variableSet.set(data.getColumnName(col), data.getValueAt(row, col));
                }
                generatedValues.add(expression.getKey().evaluate(variableSet));
            }
            boolean numeric = generatedValues.stream().allMatch(o -> o instanceof Number);
            int targetColumn = data.getOrCreateColumnIndex(expression.getValue(), !numeric);
            for (int row = 0; row < data.getRowCount(); row++) {
                data.setValueAt(generatedValues.get(row), row, targetColumn);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Expressions", description = "Each expression is applied for each row, with variables named according to the column values. " +
            "New columns are created if needed. Existing values are overwritten.")
    @JIPipeParameter("expression-list")
    @PairParameterSettings(singleRow = false, keyLabel = "Expression", valueLabel = "Column name")
    public ExpressionTableColumnGeneratorProcessorParameterList getExpressionList() {
        return expressionList;
    }

    @JIPipeParameter("expression-list")
    public void setExpressionList(ExpressionTableColumnGeneratorProcessorParameterList expressionList) {
        this.expressionList = expressionList;
    }
}
