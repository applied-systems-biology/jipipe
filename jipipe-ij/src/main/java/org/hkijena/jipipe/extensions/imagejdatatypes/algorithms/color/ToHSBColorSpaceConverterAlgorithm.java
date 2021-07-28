package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;

@JIPipeDocumentation(name = "Convert to HSB colors", description = "Converts an image into an HSB image or re-interprets existing image channels as HSB.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusColorHSBData.class, slotName = "Output")
public class ToHSBColorSpaceConverterAlgorithm extends ColorSpaceConverterAlgorithm {
    public ToHSBColorSpaceConverterAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusColorHSBData.class);
    }

    public ToHSBColorSpaceConverterAlgorithm(ColorSpaceConverterAlgorithm other) {
        super(other);
    }
}
