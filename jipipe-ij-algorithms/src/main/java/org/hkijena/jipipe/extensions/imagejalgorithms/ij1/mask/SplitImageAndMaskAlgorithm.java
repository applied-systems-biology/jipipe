package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.mask;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.LabeledImagePlusData;

@JIPipeDocumentation(name = "Split image and labels", description = "Splits a compound image and label data into their single data.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Mask")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Labels", autoCreate = true)
@JIPipeInputSlot(value = LabeledImagePlusData.class, slotName = "Compound", autoCreate = true)
public class SplitImageAndMaskAlgorithm extends JIPipeIteratingAlgorithm {
    public SplitImageAndMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitImageAndMaskAlgorithm(SplitImageAndMaskAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        LabeledImagePlusData compound = dataBatch.getInputData(getFirstInputSlot(), LabeledImagePlusData.class, progressInfo);
        dataBatch.addOutputData("Image", new ImagePlusData(compound.getImage()), progressInfo);
        dataBatch.addOutputData("Labels", new ImagePlusData(compound.getLabels()), progressInfo);
    }
}
