package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Render to RGB", description = "Converts the incoming image to RGB. This applies the LUT if any is set. " +
        "Compared to the image converter node, this operation renders the image into a representation equivalent to an image viewer, meaning " +
        "that contrast settings are preserved.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nType", aliasName = "RGB Color")
public class RenderImageToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public RenderImageToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", ImagePlusData.class)
                .addOutputSlot("Output", "", ImagePlusColorRGBData.class, "Input", ImageJAlgorithmsExtension.TO_COLOR_RGB_CONVERSION)
                .seal()
                .build());
    }

    public RenderImageToRGBAlgorithm(RenderImageToRGBAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getStackSize());
        ImageJUtils.forEachIndexedZCTSlice(img, (ip, slice) -> {
            ColorProcessor colorProcessor = new ColorProcessor(ip.getBufferedImage());
            stack.setProcessor(colorProcessor, slice.zeroSliceIndexToOneStackIndex(img));
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(new ImagePlus(img.getTitle(), stack)), progressInfo);
    }
}
