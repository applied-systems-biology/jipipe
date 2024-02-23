package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.convert;

import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Image to table column", description = "Copies the first column of an image into a new or existing column of a table. Opposite operation to 'Table column to image'.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ImagePlus2DGreyscale32FData.class, slotName = "Image", create = true, description = "The image that contains the value")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Target", optional = true, description = "Optional existing table where the image values are written into.", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class ImageToTableColumnAlgorithm extends JIPipeIteratingAlgorithm {

    private String targetColumnName = "Image data";

    public ImageToTableColumnAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImageToTableColumnAlgorithm(ImageToTableColumnAlgorithm other) {
        super(other);
        targetColumnName = other.targetColumnName;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData target = iterationStep.getInputData("Target", ResultsTableData.class, progressInfo);
        FloatProcessor processor = (FloatProcessor) iterationStep.getInputData("Image", ImagePlus2DGreyscale32FData.class, progressInfo).getImage().getProcessor();
        if (target == null) {
            target = new ResultsTableData();
            target.addRows(processor.getHeight());
        } else {
            target = new ResultsTableData(target);
        }
        int rowsToCopy = Math.min(target.getRowCount(), processor.getHeight());
        int columnIndex = target.getOrCreateColumnIndex(targetColumnName, false);
        for (int i = 0; i < rowsToCopy; i++) {
            target.setValueAt(processor.getf(i), i, columnIndex);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Target/Generated column", description = "The table column where the values will be written. Existing values will be overwritten. If the column does not exist, a new one will be generated.")
    @JIPipeParameter(value = "target-column-name", important = true)
    public String getTargetColumnName() {
        return targetColumnName;
    }

    @JIPipeParameter("target-column-name")
    public void setTargetColumnName(String targetColumnName) {
        this.targetColumnName = targetColumnName;
    }
}
