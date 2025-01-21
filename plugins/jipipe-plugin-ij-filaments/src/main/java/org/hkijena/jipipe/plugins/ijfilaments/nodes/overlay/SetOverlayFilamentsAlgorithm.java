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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.overlay;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "Set filaments overlay", description = "Set overlay filaments. Please note that such overlays are not natively supported by ImageJ.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Filaments", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
public class SetOverlayFilamentsAlgorithm extends JIPipeIteratingAlgorithm {
    public SetOverlayFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetOverlayFilamentsAlgorithm(SetOverlayFilamentsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData img = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).shallowCopy();
        Filaments3DGraphData rois = iterationStep.getInputData("Filaments", Filaments3DGraphData.class, progressInfo);
        img.removeOverlaysOfType(Filaments3DGraphData.class);
        img.addOverlay(rois);
        iterationStep.addOutputData(getFirstOutputSlot(), img, progressInfo);
    }
}
