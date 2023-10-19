package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.color;

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
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Render to RGB", description = "Converts the incoming image to RGB. This applies the LUT if any is set. " +
        "Compared to the image converter node, this operation renders the image into a representation equivalent to an image viewer, meaning " +
        "that contrast settings are preserved.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nType", aliasName = "RGB Color")
@JIPipeNodeAlias(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert", aliasName = "Convert image to RGB colors (Render)")
public class RenderImageToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public RenderImageToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RenderImageToRGBAlgorithm(RenderImageToRGBAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ImagePlus outputImage = ImageJUtils.renderToRGBWithLUTIfNeeded(inputImage, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(outputImage), progressInfo);
    }

}
