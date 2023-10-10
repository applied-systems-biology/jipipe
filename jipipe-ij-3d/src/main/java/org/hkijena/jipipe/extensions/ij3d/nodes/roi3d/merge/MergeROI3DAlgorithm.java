package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.merge;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;

import java.util.List;

@JIPipeDocumentation(name = "Merge 3D ROI", description = "Merges the input 3D ROI lists")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
public class MergeROI3DAlgorithm extends JIPipeMergingAlgorithm {

    public MergeROI3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeROI3DAlgorithm(JIPipeMergingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<ROI3DListData> inputData = dataBatch.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);
        if (!inputData.isEmpty()) {
            ROI3DListData outputData = new ROI3DListData();
            for (ROI3DListData data : inputData) {
                outputData.addAll(data);
            }
            dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
        }
    }
}
