package org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

@JIPipeDocumentation(name = "Convert spots to ROI", description = "Converts TrackMate spots into ROI")
@JIPipeNode(menuPath = "Tracking\nConvert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class ConvertSpotsToRoiNode extends JIPipeSimpleIteratingAlgorithm {
    public ConvertSpotsToRoiNode(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertSpotsToRoiNode(ConvertSpotsToRoiNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData data = dataBatch.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
        ROIListData rois = data.spotsToROIList();
        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }
}
