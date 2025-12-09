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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.convert;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;

@SetJIPipeDocumentation(name = "Convert IJ3D ROI to 2D ROI", description = "Converts a 3D ROI list into a 2D ROI list.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = Ij3dSuiteRoiListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Roi2dListData.class, name = "Output", create = true)
public class Roi3dToRoi2dConverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public Roi3dToRoi2dConverterAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Roi3dToRoi2dConverterAlgorithm(Roi3dToRoi2dConverterAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Ij3dSuiteRoiListData inputRois = iterationStep.getInputData(getFirstInputSlot(), Ij3dSuiteRoiListData.class, progressInfo);
        Roi2dListData outputRois = inputRois.toRoi2d(progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), outputRois, progressInfo);
    }
}
