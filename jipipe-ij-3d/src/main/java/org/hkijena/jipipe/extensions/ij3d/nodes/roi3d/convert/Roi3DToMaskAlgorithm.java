package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.convert;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

@JIPipeDocumentation(name = "3D ROI to mask", description = "Converts 3D ROI lists to a mask")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true, description = "Optional reference image that determines the size of the output")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
public class Roi3DToMaskAlgorithm extends JIPipeIteratingAlgorithm {
    public Roi3DToMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Roi3DToMaskAlgorithm(Roi3DToMaskAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi3DListData = dataBatch.getInputData("ROI", ROI3DListData.class, progressInfo);
        ImagePlusData referenceImage = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);
        ImagePlus outputImage = roi3DListData.toMask(referenceImage != null ? referenceImage.getImage() : null, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(outputImage), progressInfo);
    }
}
