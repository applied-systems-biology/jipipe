package org.hkijena.jipipe.extensions.ij3d.nodes.overlay;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "Remove 3D overlay", description = "Remove overlay ROIs")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nOverlay")
public class RemoveOverlay3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public RemoveOverlay3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveOverlay3DAlgorithm(RemoveOverlay3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).shallowCopy();
        img.removeOverlaysOfType(ROI3DListData.class);
        iterationStep.addOutputData(getFirstOutputSlot(), img, progressInfo);
    }
}
