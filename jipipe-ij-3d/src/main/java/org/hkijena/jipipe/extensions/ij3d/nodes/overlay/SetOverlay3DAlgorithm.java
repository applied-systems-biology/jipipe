package org.hkijena.jipipe.extensions.ij3d.nodes.overlay;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "Set 3D overlay", description = "Set overlay ROIs. Please note that 3D overlays are not natively supported by ImageJ and cann")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
public class SetOverlay3DAlgorithm extends JIPipeIteratingAlgorithm {
    public SetOverlay3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetOverlay3DAlgorithm(SetOverlay3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData img = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).shallowCopy();
        ROI3DListData rois = iterationStep.getInputData("ROI", ROI3DListData.class, progressInfo);
        img.removeOverlaysOfType(ROI3DListData.class);
        img.addOverlay(rois);
        iterationStep.addOutputData(getFirstOutputSlot(), img, progressInfo);
    }
}
