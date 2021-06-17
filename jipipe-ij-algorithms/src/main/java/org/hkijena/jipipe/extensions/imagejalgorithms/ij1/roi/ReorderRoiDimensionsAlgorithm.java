package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;

/**
 * Algorithm that reorders Hyperstack dimensions
 */
@JIPipeDocumentation(name = "Reorder ROI dimensions", description = "Reorders dimensions of all the ROI contained in the ROI lists. " +
        "Unlike the equivalent method for images, this node allows to have non-unique mappings.")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
public class ReorderRoiDimensionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private HyperstackDimension targetZ = HyperstackDimension.Depth;
    private HyperstackDimension targetC = HyperstackDimension.Channel;
    private HyperstackDimension targetT = HyperstackDimension.Frame;

    public ReorderRoiDimensionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ReorderRoiDimensionsAlgorithm(ReorderRoiDimensionsAlgorithm other) {
        super(other);
        this.targetZ = other.targetZ;
        this.targetC = other.targetC;
        this.targetT = other.targetT;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData rois = new ROIListData(dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo));

        for (Roi roi : rois) {
            int depth = roi.getZPosition();
            int channels = roi.getCPosition();
            int frames = roi.getTPosition();
            int newDepth = depth;
            int newChannels = channels;
            int newFrames = frames;

            switch (targetZ) {
                case Channel:
                    newDepth = channels;
                    break;
                case Frame:
                    newDepth = frames;
                    break;
            }
            switch (targetC) {
                case Depth:
                    newChannels = depth;
                    break;
                case Frame:
                    newChannels = frames;
                    break;
            }
            switch (targetT) {
                case Channel:
                    newFrames = channels;
                    break;
                case Depth:
                    newFrames = depth;
                    break;
            }

            roi.setPosition(newChannels, newDepth, newFrames);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @JIPipeDocumentation(name = "Copy Z to ...", description = "Determines how the Z dimension is re-mapped.")
    @JIPipeParameter("target-z")
    public HyperstackDimension getTargetZ() {
        return targetZ;
    }

    @JIPipeParameter("target-z")
    public void setTargetZ(HyperstackDimension targetZ) {
        this.targetZ = targetZ;
    }

    @JIPipeDocumentation(name = "Copy C to ...", description = "Determines how the C (channel) dimension is re-mapped.")
    @JIPipeParameter("target-c")
    public HyperstackDimension getTargetC() {
        return targetC;
    }

    @JIPipeParameter("target-c")
    public void setTargetC(HyperstackDimension targetC) {
        this.targetC = targetC;
    }

    @JIPipeDocumentation(name = "Copy T to ...", description = "Determines how the T (time) dimension is re-mapped.")
    @JIPipeParameter("target-t")
    public HyperstackDimension getTargetT() {
        return targetT;
    }

    @JIPipeParameter("target-t")
    public void setTargetT(HyperstackDimension targetT) {
        this.targetT = targetT;
    }
}
