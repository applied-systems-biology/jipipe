package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.merge;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;

import java.util.List;

@SetJIPipeDocumentation(name = "Merge 3D ROI", description = "Merges the input 3D ROI lists")
@DefineJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", create = true)
public class MergeROI3DAlgorithm extends JIPipeMergingAlgorithm {

    public MergeROI3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeROI3DAlgorithm(JIPipeMergingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<ROI3DListData> inputData = iterationStep.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);
        if (!inputData.isEmpty()) {
            ROI3DListData outputData = new ROI3DListData();
            for (ROI3DListData data : inputData) {
                outputData.addAll(data);
            }
            iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
        }
    }
}
