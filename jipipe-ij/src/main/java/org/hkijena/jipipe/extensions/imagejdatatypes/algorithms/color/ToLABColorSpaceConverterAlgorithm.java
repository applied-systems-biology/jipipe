package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorLABData;

@SetJIPipeDocumentation(name = "Convert color image to LAB colors", description = "Converts an image into an LAB image or re-interprets existing image channels as LAB. " +
        "Please note that this node is designed for color images.")
@DefineJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@AddJIPipeOutputSlot(value = ImagePlusColorLABData.class, slotName = "Output")
public class ToLABColorSpaceConverterAlgorithm extends ColorSpaceConverterAlgorithm {
    public ToLABColorSpaceConverterAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusColorLABData.class);
    }

    public ToLABColorSpaceConverterAlgorithm(ColorSpaceConverterAlgorithm other) {
        super(other);
    }
}
