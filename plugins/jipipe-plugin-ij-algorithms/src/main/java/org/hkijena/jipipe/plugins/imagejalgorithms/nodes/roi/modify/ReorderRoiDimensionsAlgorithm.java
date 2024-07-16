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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.modify;

import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.HyperstackDimension;

/**
 * Algorithm that reorders Hyperstack dimensions
 */
@SetJIPipeDocumentation(name = "Reorder 2D ROI dimensions", description = "Reorders dimensions of all the ROI contained in the ROI lists. " +
        "Unlike the equivalent method for images, this node allows to have non-unique mappings.")
@AddJIPipeInputSlot(value = ROIListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData rois = new ROIListData(iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo));

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

        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Copy Z to ...", description = "Determines how the Z dimension is re-mapped.")
    @JIPipeParameter("target-z")
    public HyperstackDimension getTargetZ() {
        return targetZ;
    }

    @JIPipeParameter("target-z")
    public void setTargetZ(HyperstackDimension targetZ) {
        this.targetZ = targetZ;
    }

    @SetJIPipeDocumentation(name = "Copy C to ...", description = "Determines how the C (channel) dimension is re-mapped.")
    @JIPipeParameter("target-c")
    public HyperstackDimension getTargetC() {
        return targetC;
    }

    @JIPipeParameter("target-c")
    public void setTargetC(HyperstackDimension targetC) {
        this.targetC = targetC;
    }

    @SetJIPipeDocumentation(name = "Copy T to ...", description = "Determines how the T (time) dimension is re-mapped.")
    @JIPipeParameter("target-t")
    public HyperstackDimension getTargetT() {
        return targetT;
    }

    @JIPipeParameter("target-t")
    public void setTargetT(HyperstackDimension targetT) {
        this.targetT = targetT;
    }
}
