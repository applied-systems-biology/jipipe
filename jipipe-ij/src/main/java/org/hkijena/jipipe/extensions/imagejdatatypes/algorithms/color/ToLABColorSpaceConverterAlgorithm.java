package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorLABData;

@JIPipeDocumentation(name = "Convert color image to LAB colors", description = "Converts an image into an LAB image or re-interprets existing image channels as LAB. " +
        "Please note that this node is designed for color images.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusColorLABData.class, slotName = "Output")
public class ToLABColorSpaceConverterAlgorithm extends ColorSpaceConverterAlgorithm {
    public ToLABColorSpaceConverterAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusColorLABData.class);
    }

    public ToLABColorSpaceConverterAlgorithm(ColorSpaceConverterAlgorithm other) {
        super(other);
    }
}
