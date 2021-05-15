package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.mask;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.MaskedImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

@JIPipeDocumentation(name = "Split image and mask", description = "Splits a compound image and mask data into their single data.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Mask")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)
@JIPipeInputSlot(value = MaskedImagePlusData.class, slotName = "Compound", autoCreate = true)
public class SplitImageAndMaskAlgorithm extends JIPipeIteratingAlgorithm {
    public SplitImageAndMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitImageAndMaskAlgorithm(SplitImageAndMaskAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        MaskedImagePlusData compound = dataBatch.getInputData(getFirstInputSlot(), MaskedImagePlusData.class, progressInfo);
        dataBatch.addOutputData("Image", new ImagePlusData(compound.getImage()), progressInfo);
        dataBatch.addOutputData("Mask", new ImagePlusGreyscaleMaskData(compound.getMask()), progressInfo);
    }
}
