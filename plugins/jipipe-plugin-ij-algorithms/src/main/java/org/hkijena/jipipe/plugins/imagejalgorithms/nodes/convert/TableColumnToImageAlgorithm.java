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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.convert;

import ij.ImagePlus;
import ij.process.FloatProcessor;
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
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

@SetJIPipeDocumentation(name = "Table column to image", description = "Converts a selected numeric table column into an image with 1px width and a height based on the number of rows. Opposite operation to 'Image to table column'.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DGreyscale32FData.class, name = "Output", create = true)
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
        TableColumnData tableColumn = selectedColumn.pickOrGenerateColumn(tableData, new JIPipeExpressionVariablesMap());
        FloatProcessor processor = new FloatProcessor(1, tableColumn.getRows());
        for (int i = 0; i < tableColumn.getRows(); i++) {
            processor.setf(i, (float) tableColumn.getRowAsDouble(i));
        }
        ImagePlus imagePlus = new ImagePlus(selectedColumn.getValue().getExpression(), processor);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscale32FData(imagePlus), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Column to convert", description = "The column to be converted into an image")
    @JIPipeParameter(value = "selected-column", important = true)
    public TableColumnSourceExpressionParameter getSelectedColumn() {
        return selectedColumn;
    }

    @JIPipeParameter("selected-column")
    public void setSelectedColumn(TableColumnSourceExpressionParameter selectedColumn) {
        this.selectedColumn = selectedColumn;
    }
}
