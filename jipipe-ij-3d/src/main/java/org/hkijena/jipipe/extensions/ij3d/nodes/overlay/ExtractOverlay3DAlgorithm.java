package org.hkijena.jipipe.extensions.ij3d.nodes.overlay;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "Extract 3D overlay", description = "Extract overlay 3D ROIs")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nOverlay")
public class ExtractOverlay3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ExtractOverlay3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractOverlay3DAlgorithm(ExtractOverlay3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ROI3DListData rois = new ROI3DListData();
        for (ROI3DListData data : image.extractOverlaysOfType(ROI3DListData.class)) {
            rois.addAll(data);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }
}
