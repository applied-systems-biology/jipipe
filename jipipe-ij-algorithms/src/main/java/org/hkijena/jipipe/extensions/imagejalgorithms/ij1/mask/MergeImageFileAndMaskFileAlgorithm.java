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
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.LabeledImageFileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.LabeledImagePlusData;

@JIPipeDocumentation(name = "Associate image and label files", description = "Associates an image and a label file, creating a compound data.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Mask")
@JIPipeInputSlot(value = FileData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = FileData.class, slotName = "Labels", autoCreate = true)
@JIPipeOutputSlot(value = LabeledImageFileData.class, slotName = "Compound", autoCreate = true)
public class MergeImageFileAndMaskFileAlgorithm extends JIPipeIteratingAlgorithm {
    public MergeImageFileAndMaskFileAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeImageFileAndMaskFileAlgorithm(MergeImageFileAndMaskFileAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData image = dataBatch.getInputData("Image", FileData.class, progressInfo);
        FileData mask = dataBatch.getInputData("Labels", FileData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new LabeledImageFileData(image.getPath(), mask.getPath()), progressInfo);
    }
}
