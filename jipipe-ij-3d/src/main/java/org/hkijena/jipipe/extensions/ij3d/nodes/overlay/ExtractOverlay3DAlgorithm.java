package org.hkijena.jipipe.extensions.ij3d.nodes.overlay;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "Extract 3D overlay", description = "Extract overlay 3D ROIs")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nOverlay")
public class ExtractOverlay3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ExtractOverlay3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractOverlay3DAlgorithm(ExtractOverlay3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ROI3DListData rois = new ROI3DListData();
        for (ROI3DListData data : image.extractOverlaysOfType(ROI3DListData.class)) {
            rois.addAll(data);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }
}
