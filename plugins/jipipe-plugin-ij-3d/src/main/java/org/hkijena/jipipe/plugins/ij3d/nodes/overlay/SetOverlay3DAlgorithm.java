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

package org.hkijena.jipipe.plugins.ij3d.nodes.overlay;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.ij3d.datatypes.IJ3DROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "IJ3D Set 3D overlay", description = "Set overlay ROIs. Please note that 3D overlays are not natively supported by ImageJ.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = IJ3DROIListData.class, name = "ROI", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@MarkNodeAsUnstable
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
        IJ3DROIListData rois = iterationStep.getInputData("ROI", IJ3DROIListData.class, progressInfo);
        img.removeOverlaysOfType(IJ3DROIListData.class);
        img.addOverlay(rois);
        iterationStep.addOutputData(getFirstOutputSlot(), img, progressInfo);
    }
}
