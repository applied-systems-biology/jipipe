package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.convert;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;

@JIPipeDocumentation(name = "Convert image to 32-bit", description = "Converts an image into 32-bit")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nType", aliasName = "32-bit")
public class ConvertImageTo32BitAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ConvertImageTo32BitAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertImageTo32BitAlgorithm(ConvertImageTo32BitAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(image), progressInfo);
    }
}
