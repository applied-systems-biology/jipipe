package org.hkijena.jipipe.extensions.imp.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageData;
import org.hkijena.jipipe.extensions.imp.utils.ImpImageDataBuilder;
import org.hkijena.jipipe.extensions.imp.utils.ImpImageUtils;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.image.BufferedImage;

@SetJIPipeDocumentation(name = "Split alpha channel", description = "Splits the alpha channel of IMP images into a dedicated image")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "IMP")
@AddJIPipeInputSlot(value = ImpImageData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImpImageData.class, slotName = "RGB", create = true)
@AddJIPipeOutputSlot(value = ImpImageData.class, slotName = "Alpha", create = true)
public class SplitImpAlphaChannelAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public SplitImpAlphaChannelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitImpAlphaChannelAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImpImageData inputData = iterationStep.getInputData(getFirstInputSlot(), ImpImageData.class, progressInfo);
        ImpImageDataBuilder rgbBuilder = new ImpImageDataBuilder();
        ImpImageDataBuilder maskBuilder = new ImpImageDataBuilder();
        ImpImageUtils.forEachIndexedZCTSlice(inputData, (ip, index) -> {
            rgbBuilder.put(index, BufferedImageUtils.copyBufferedImageToRGB(ip));
            maskBuilder.put(index, BufferedImageUtils.extractAlpha(ip));
        }, progressInfo);
        iterationStep.addOutputData("RGB",rgbBuilder.build(), progressInfo);
        iterationStep.addOutputData("Alpha", maskBuilder.build(), progressInfo);
    }
}
