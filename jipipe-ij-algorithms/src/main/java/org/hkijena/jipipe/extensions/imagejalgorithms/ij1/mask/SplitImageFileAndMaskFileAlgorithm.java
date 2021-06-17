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
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.LabeledImageFileData;

@JIPipeDocumentation(name = "Split image and label files", description = "Splits a compound image and label file data into their single data.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Mask")
@JIPipeOutputSlot(value = FileData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Labels", autoCreate = true)
@JIPipeInputSlot(value = LabeledImageFileData.class, slotName = "Compound", autoCreate = true)
public class SplitImageFileAndMaskFileAlgorithm extends JIPipeIteratingAlgorithm {
    public SplitImageFileAndMaskFileAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitImageFileAndMaskFileAlgorithm(SplitImageFileAndMaskFileAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        LabeledImageFileData compound = dataBatch.getInputData(getFirstInputSlot(), LabeledImageFileData.class, progressInfo);
        dataBatch.addOutputData("Image", new FileData(compound.getPath()), progressInfo);
        dataBatch.addOutputData("Labels", new FileData(compound.getLabelPath()), progressInfo);
    }
}
