package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.overlay;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

@JIPipeDocumentation(name = "Set overlay", description = "Set overlay ROIs")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nOverlay", aliasName = "Set Overlay")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nOverlay", aliasName = "From ROI Manager")
public class SetOverlayAlgorithm extends JIPipeIteratingAlgorithm {
    public SetOverlayAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetOverlayAlgorithm(SetOverlayAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).getDuplicateImage();
        ROIListData rois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        Overlay overlay = new Overlay();
        for (Roi roi : rois) {
            overlay.add(roi);
        }
        img.setOverlay(overlay);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }
}
