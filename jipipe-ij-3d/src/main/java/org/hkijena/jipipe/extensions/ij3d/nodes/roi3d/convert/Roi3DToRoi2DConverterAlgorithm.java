package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.convert;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

@JIPipeDocumentation(name = "Convert 3D ROI to 2D ROI", description = "Converts a 3D ROI list into a 2D ROI list.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class Roi3DToRoi2DConverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public Roi3DToRoi2DConverterAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Roi3DToRoi2DConverterAlgorithm(Roi3DToRoi2DConverterAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData inputRois = dataBatch.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);
        ROIListData outputRois = inputRois.toRoi2D(progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), outputRois, progressInfo);
    }
}
