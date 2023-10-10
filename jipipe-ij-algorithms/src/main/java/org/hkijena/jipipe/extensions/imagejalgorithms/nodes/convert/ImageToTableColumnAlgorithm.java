package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.convert;

import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Image to table column", description = "Copies the first column of an image into a new or existing column of a table. Opposite operation to 'Table column to image'.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ImagePlus2DGreyscale32FData.class, slotName = "Image", autoCreate = true, description = "The image that contains the value")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Target", optional = true, description = "Optional existing table where the image values are written into.", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData target = dataBatch.getInputData("Target", ResultsTableData.class, progressInfo);
        FloatProcessor processor = (FloatProcessor) dataBatch.getInputData("Image", ImagePlus2DGreyscale32FData.class, progressInfo).getImage().getProcessor();
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
        dataBatch.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }

    @JIPipeDocumentation(name = "Target/Generated column", description = "The table column where the values will be written. Existing values will be overwritten. If the column does not exist, a new one will be generated.")
    @JIPipeParameter(value = "target-column-name", important = true)
    public String getTargetColumnName() {
        return targetColumnName;
    }

    @JIPipeParameter("target-column-name")
    public void setTargetColumnName(String targetColumnName) {
        this.targetColumnName = targetColumnName;
    }
}
