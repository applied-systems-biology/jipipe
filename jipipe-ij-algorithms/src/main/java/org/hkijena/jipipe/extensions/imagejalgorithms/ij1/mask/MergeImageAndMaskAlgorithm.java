package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.mask;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.MaskedImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

@JIPipeDocumentation(name = "Associate image and mask", description = "Associates an image and a mask, creating a compound data.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Mask")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)
@JIPipeOutputSlot(value = MaskedImagePlusData.class, slotName = "Compound", autoCreate = true)
public class MergeImageAndMaskAlgorithm extends JIPipeIteratingAlgorithm {
    public MergeImageAndMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeImageAndMaskAlgorithm(MergeImageAndMaskAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
        ImagePlusData mask = dataBatch.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new MaskedImagePlusData(image.getImage(), mask.getImage()), progressInfo);
    }
}
