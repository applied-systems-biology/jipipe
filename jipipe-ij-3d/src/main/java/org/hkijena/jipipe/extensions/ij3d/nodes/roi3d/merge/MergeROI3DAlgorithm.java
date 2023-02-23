package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.merge;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
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
        if(!inputData.isEmpty()) {
            // TODO merge units?
            ROI3DListData outputData = inputData.get(0).newWithSameCalibration();
            for (ROI3DListData data : inputData) {
                outputData.addObjects(data.getObjectsList());
            }
            dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
        }
    }
}
