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
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageData;
import org.hkijena.jipipe.extensions.imp.utils.ImpImageDataBuilder;
import org.hkijena.jipipe.extensions.imp.utils.ImpImageUtils;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

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

        ImpImageData inputImage = iterationStep.getInputData("Image", ImpImageData.class, progressInfo);
        ImpImageData maskImage = iterationStep.getInputData("Alpha", ImpImageData.class, progressInfo);
        ImpImageDataBuilder builder = new ImpImageDataBuilder();

        ImpImageUtils.forEachIndexedZCTSlice(inputImage, (image, index) -> {
            BufferedImage mask = maskImage.getImageOrExpand(index);
            BufferedImage result = BufferedImageUtils.setAlpha(image, mask);
            builder.put(index, result);
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), builder.build(), progressInfo);
    }
}
