package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

@JIPipeDocumentation(name = "Convert to RGB colors", description = "Converts an image into an RGB image or re-interprets existing image channels as RGB.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Color\nConvert")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class ToRGBColorSpaceConverterAlgorithm extends ColorSpaceConverterAlgorithm {
    public ToRGBColorSpaceConverterAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusColorRGBData.class);
    }

    public ToRGBColorSpaceConverterAlgorithm(ColorSpaceConverterAlgorithm other) {
        super(other);
    }
}
