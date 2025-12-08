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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.merge;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.plugins.ij3d.datatypes.IJ3DROIListData;

import java.util.List;

@SetJIPipeDocumentation(name = "Merge IJ3D ROI", description = "Merges the input 3D ROI lists")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = IJ3DROIListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = IJ3DROIListData.class, name = "Output", create = true)
public class MergeROI3DAlgorithm extends JIPipeMergingAlgorithm {

    public MergeROI3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeROI3DAlgorithm(JIPipeMergingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<IJ3DROIListData> inputData = iterationStep.getInputData(getFirstInputSlot(), IJ3DROIListData.class, progressInfo);
        if (!inputData.isEmpty()) {
            IJ3DROIListData outputData = new IJ3DROIListData();
            for (IJ3DROIListData data : inputData) {
                outputData.addAll(data);
            }
            iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
        }
    }
}
