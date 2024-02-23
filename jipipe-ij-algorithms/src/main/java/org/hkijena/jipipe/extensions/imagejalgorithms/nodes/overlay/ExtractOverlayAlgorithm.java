package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.overlay;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

@SetJIPipeDocumentation(name = "Extract overlay", description = "Extract overlay ROIs")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nOverlay", aliasName = "To ROI Manager")
public class ExtractOverlayAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ExtractOverlayAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractOverlayAlgorithm(ExtractOverlayAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ROIListData rois = new ROIListData();
        if (img.getOverlay() != null) {
            for (Roi roi : img.getOverlay()) {
                rois.add(roi);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }
}
