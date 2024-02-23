package org.hkijena.jipipe.extensions.ij3d.nodes.overlay;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "Set 3D overlay", description = "Set overlay ROIs. Please note that 3D overlays are not natively supported by ImageJ and cann")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
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
