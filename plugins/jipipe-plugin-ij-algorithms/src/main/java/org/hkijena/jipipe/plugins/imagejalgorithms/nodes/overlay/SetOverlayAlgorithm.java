/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.overlay;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;

@SetJIPipeDocumentation(name = "Set overlay", description = "Set overlay ROIs")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nOverlay", aliasName = "Set Overlay")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nOverlay", aliasName = "From ROI Manager")
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
