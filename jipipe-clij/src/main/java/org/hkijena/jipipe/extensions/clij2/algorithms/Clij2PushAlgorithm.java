package org.hkijena.jipipe.extensions.clij2.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "CLIJ2 Push to GPU", description = "Converts an image into a GPU image managed by CLIJ")
@AddJIPipeInputSlot(slotName = "Input", value = ImagePlusData.class, create = true)
@AddJIPipeOutputSlot(slotName = "Output", value = CLIJImageData.class, create = true)
@DefineJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "CLIJ")
public class Clij2PushAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public Clij2PushAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Clij2PushAlgorithm(Clij2PushAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), JIPipe.getDataTypes().convert(inputData, CLIJImageData.class, progressInfo), progressInfo);
    }
}
