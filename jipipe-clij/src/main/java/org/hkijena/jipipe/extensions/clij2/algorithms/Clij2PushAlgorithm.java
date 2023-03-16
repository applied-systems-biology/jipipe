package org.hkijena.jipipe.extensions.clij2.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "CLIJ2 Push to GPU", description = "Converts an image into a GPU image managed by CLIJ")
@JIPipeInputSlot(slotName = "Input", value = ImagePlusData.class, autoCreate = true)
@JIPipeOutputSlot(slotName = "Output", value = CLIJImageData.class, autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "CLIJ")
public class Clij2PushAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public Clij2PushAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Clij2PushAlgorithm(Clij2PushAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.getDataTypes().convert(inputData, CLIJImageData.class, progressInfo), progressInfo);
    }
}
