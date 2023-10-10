package org.hkijena.jipipe.extensions.tables.nodes.filter;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Select table rows", description = "Allows to select/slice the only specific rows from the input table.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class SliceTableRowsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntegerRange limit = new IntegerRange("0-10");

    public SliceTableRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SliceTableRowsAlgorithm(SliceTableRowsAlgorithm other) {
        super(other);
        this.limit = new IntegerRange(other.limit);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputTable = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        ResultsTableData outputTable = inputTable.getRows(limit.getIntegers(0, inputTable.getRowCount(), variables));
        dataBatch.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @JIPipeDocumentation(name = "Limit", description = "Determines which row indices are passed to the output. The first index is zero.")
    @JIPipeParameter("limit")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public IntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    public void setLimit(IntegerRange limit) {
        this.limit = limit;
    }
}
