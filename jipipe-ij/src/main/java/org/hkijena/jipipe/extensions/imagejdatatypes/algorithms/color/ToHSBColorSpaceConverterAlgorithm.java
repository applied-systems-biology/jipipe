package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;

@SetJIPipeDocumentation(name = "Convert color image to HSB colors", description = "Converts an image into an HSB image or re-interprets existing image channels as HSB. " +
        "Please note that this node is designed for color images.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@AddJIPipeOutputSlot(value = ImagePlusColorHSBData.class, slotName = "Output")
public class ToHSBColorSpaceConverterAlgorithm extends ColorSpaceConverterAlgorithm {
    public ToHSBColorSpaceConverterAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusColorHSBData.class);
    }

    public ToHSBColorSpaceConverterAlgorithm(ColorSpaceConverterAlgorithm other) {
        super(other);
    }
}
