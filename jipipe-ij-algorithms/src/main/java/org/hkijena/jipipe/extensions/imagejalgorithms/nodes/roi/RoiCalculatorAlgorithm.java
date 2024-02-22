/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi;

import ij.gui.Roi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.util.LogicalOperation;

import java.util.List;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "ROI calculator", description = "Applies logical operations to the input ROI list. The logical operations are applied to " +
        "the whole list, meaning that an AND operation will create the union of all ROI in the list. If you want to apply the operation only to a sub-set of ROI," +
        " preprocess using a ROI splitter algorithm.")
@DefineJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
public class RoiCalculatorAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private LogicalOperation operation = LogicalOperation.LogicalAnd;
    private boolean applyPerSlice = false;
    private boolean applyPerChannel = false;
    private boolean applyPerFrame = false;
    private boolean splitAfterwards = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RoiCalculatorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RoiCalculatorAlgorithm(RoiCalculatorAlgorithm other) {
        super(other);
        this.operation = other.operation;
        this.splitAfterwards = other.splitAfterwards;
        this.applyPerChannel = other.applyPerChannel;
        this.applyPerFrame = other.applyPerFrame;
        this.applyPerSlice = other.applyPerSlice;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData inputData = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        Map<ImageSliceIndex, List<Roi>> grouped = inputData.groupByPosition(applyPerSlice, applyPerChannel, applyPerFrame);
        ROIListData outputData = new ROIListData();
        for (Map.Entry<ImageSliceIndex, List<Roi>> entry : grouped.entrySet()) {
            ROIListData data = new ROIListData(entry.getValue());
            switch (operation) {
                case LogicalAnd:
                    data.logicalAnd();
                    break;
                case LogicalOr:
                    data.logicalOr();
                    break;
                case LogicalXor:
                    data.logicalXor();
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + operation);
            }
            if (splitAfterwards)
                data.splitAll();
            for (Roi roi : data) {
                roi.setPosition(entry.getKey().getC() + 1,
                        entry.getKey().getZ() + 1,
                        entry.getKey().getT() + 1);
            }
            outputData.addAll(data);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Operation", description = "The operation to apply on the list of ROI")
    @JIPipeParameter("operation")
    public LogicalOperation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(LogicalOperation operation) {
        this.operation = operation;
    }

    @SetJIPipeDocumentation(name = "Split after operation", description = "If enabled, ROI are split into connected components after the operation is applied. " +
            "This is useful as some operations create only one ROI output with multiple unconnected components.")
    @JIPipeParameter("split-afterwards")
    public boolean isSplitAfterwards() {
        return splitAfterwards;
    }

    @JIPipeParameter("split-afterwards")
    public void setSplitAfterwards(boolean splitAfterwards) {
        this.splitAfterwards = splitAfterwards;
    }

    @SetJIPipeDocumentation(name = "Apply per slice", description = "If true, the operation is applied for each Z-slice separately. If false, all Z-slices are put together.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @SetJIPipeDocumentation(name = "Apply per channel", description = "If true, the operation is applied for each channel-slice separately. If false, all channel-slices are put together. " +
            "Please note that 'Channel' does not refer to a pixel channel like Red in RGB.")
    @JIPipeParameter("apply-per-channel")
    public boolean isApplyPerChannel() {
        return applyPerChannel;
    }

    @JIPipeParameter("apply-per-channel")
    public void setApplyPerChannel(boolean applyPerChannel) {
        this.applyPerChannel = applyPerChannel;
    }

    @SetJIPipeDocumentation(name = "Apply per frame", description = "If true, the operation is applied for each frame separately. If false, all frames are put together.")
    @JIPipeParameter("apply-per-frame")
    public boolean isApplyPerFrame() {
        return applyPerFrame;
    }

    @JIPipeParameter("apply-per-frame")
    public void setApplyPerFrame(boolean applyPerFrame) {
        this.applyPerFrame = applyPerFrame;
    }

}
