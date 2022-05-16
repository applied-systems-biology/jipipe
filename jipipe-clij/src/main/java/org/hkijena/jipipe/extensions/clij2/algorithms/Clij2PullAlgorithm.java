package org.hkijena.jipipe.extensions.clij2.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "CLIJ2 Pull from GPU", description = "Converts a GPU image into a non-CLIJ image")
@JIPipeInputSlot(slotName = "Input", value = CLIJImageData.class, autoCreate = true)
@JIPipeOutputSlot(slotName = "Output", value = ImagePlusData.class, autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "CLIJ")
public class Clij2PullAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public Clij2PullAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Clij2PullAlgorithm(Clij2PullAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJImageData inputData = dataBatch.getInputData(getFirstInputSlot(), CLIJImageData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.getDataTypes().convert(inputData, ImagePlusData.class), progressInfo);
    }
}