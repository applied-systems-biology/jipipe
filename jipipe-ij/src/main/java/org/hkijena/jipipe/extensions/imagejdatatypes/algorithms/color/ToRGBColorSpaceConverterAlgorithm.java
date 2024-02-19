package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

@SetJIPipeDocumentation(name = "Convert color image to RGB colors", description = "Converts an image into an RGB image or re-interprets existing image channels as RGB. " +
        "Please note that this node is designed for color images. " +
        "Use 'Render to RGB'/'Convert image to RGB colors (Render)' if you have issues.")
@DefineJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class ToRGBColorSpaceConverterAlgorithm extends ColorSpaceConverterAlgorithm {
    public ToRGBColorSpaceConverterAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusColorRGBData.class);
    }

    public ToRGBColorSpaceConverterAlgorithm(ColorSpaceConverterAlgorithm other) {
        super(other);
    }
}
