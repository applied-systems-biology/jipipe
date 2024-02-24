package org.hkijena.jipipe.extensions.imp.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageData;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.image.BufferedImage;

@SetJIPipeDocumentation(name = "Set alpha channel from mask", description = "Sets the alpha channel of an IMP image from a mask")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "IMP")
@AddJIPipeInputSlot(value = ImpImageData.class, slotName = "Image", create = true)
@AddJIPipeInputSlot(value = ImpImageData.class, slotName = "Alpha", create = true)
@AddJIPipeOutputSlot(value = ImpImageData.class, slotName = "Output", create = true)
public class SetImpAlphaChannelAlgorithm extends JIPipeIteratingAlgorithm {
    public SetImpAlphaChannelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetImpAlphaChannelAlgorithm(JIPipeIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        BufferedImage image = iterationStep.getInputData("Image", ImpImageData.class, progressInfo).getImage();
        BufferedImage mask = iterationStep.getInputData("Alpha", ImpImageData.class, progressInfo).getImage();
        BufferedImage result = BufferedImageUtils.setAlpha(image, mask);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImpImageData(result), progressInfo);
    }
}
