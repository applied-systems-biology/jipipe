package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.convert;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

@JIPipeDocumentation(name = "Convert image to RGB", description = "Converts an image into RGB colors")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
public class ConvertImageToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ConvertImageToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertImageToRGBAlgorithm(ConvertImageToRGBAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(image), progressInfo);
    }
}
