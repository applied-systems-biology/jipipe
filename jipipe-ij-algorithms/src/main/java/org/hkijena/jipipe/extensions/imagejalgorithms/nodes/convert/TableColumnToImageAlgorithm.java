package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.convert;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

@JIPipeDocumentation(name = "Table column to image", description = "Converts a selected numeric table column into an image with 1px width and a height based on the number of rows. Opposite operation to 'Image to table column'.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus2DGreyscale32FData.class, slotName = "Output", autoCreate = true)
public class TableColumnToImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceExpressionParameter selectedColumn = new TableColumnSourceExpressionParameter();

    public TableColumnToImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TableColumnToImageAlgorithm(TableColumnToImageAlgorithm other) {
        super(other);
        this.selectedColumn = new TableColumnSourceExpressionParameter(other.selectedColumn);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData tableData = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        TableColumn tableColumn = selectedColumn.pickOrGenerateColumn(tableData);
        FloatProcessor processor = new FloatProcessor(1, tableColumn.getRows());
        for (int i = 0; i < tableColumn.getRows(); i++) {
            processor.setf(i, (float) tableColumn.getRowAsDouble(i));
        }
        ImagePlus imagePlus = new ImagePlus(selectedColumn.getValue().getExpression(), processor);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscale32FData(imagePlus), progressInfo);
    }

    @JIPipeDocumentation(name = "Column to convert", description = "The column to be converted into an image")
    @JIPipeParameter(value = "selected-column", important = true)
    public TableColumnSourceExpressionParameter getSelectedColumn() {
        return selectedColumn;
    }

    @JIPipeParameter("selected-column")
    public void setSelectedColumn(TableColumnSourceExpressionParameter selectedColumn) {
        this.selectedColumn = selectedColumn;
    }
}
