package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.SliceIndex;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "ROI calculator", description = "Applies logical operations to the input ROI list. The logical operations are applied to " +
        "the whole list, meaning that an AND operation will create the intersection of all ROI in the list. If you want to apply the operation only to a sub-set of ROI," +
        " preprocess using a ROI splitter algorithm.")
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class RoiCalculatorAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private Operation operation = Operation.LogicalAnd;
    private boolean applyPerSlice = false;
    private boolean applyPerChannel = false;
    private boolean applyPerFrame = false;
    private boolean splitAfterwards = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public RoiCalculatorAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = dataInterface.getInputData(getFirstInputSlot(), ROIListData.class);
        Map<SliceIndex, List<Roi>> grouped = inputData.groupByPosition(applyPerSlice, applyPerChannel, applyPerFrame);
        ROIListData outputData = new ROIListData();
        for (Map.Entry<SliceIndex, List<Roi>> entry : grouped.entrySet()) {
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
        dataInterface.addOutputData(getFirstOutputSlot(), outputData);
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Operation", description = "The operation to apply on the list of ROI")
    @ACAQParameter("operation")
    public Operation getOperation() {
        return operation;
    }

    @ACAQParameter("operation")
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    @ACAQDocumentation(name = "Split after operation", description = "If enabled, ROI are split into connected components after the operation is applied. " +
            "This is useful as some operations create only one ROI output with multiple unconnected components.")
    @ACAQParameter("split-afterwards")
    public boolean isSplitAfterwards() {
        return splitAfterwards;
    }

    @ACAQParameter("split-afterwards")
    public void setSplitAfterwards(boolean splitAfterwards) {
        this.splitAfterwards = splitAfterwards;
    }

    @ACAQDocumentation(name = "Apply per slice", description = "If true, the operation is applied for each Z-slice separately. If false, all Z-slices are put together.")
    @ACAQParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @ACAQParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @ACAQDocumentation(name = "Apply per channel", description = "If true, the operation is applied for each channel-slice separately. If false, all channel-slices are put together. " +
            "Please note that 'Channel' does not refer to a pixel channel like Red in RGB.")
    @ACAQParameter("apply-per-channel")
    public boolean isApplyPerChannel() {
        return applyPerChannel;
    }

    @ACAQParameter("apply-per-channel")
    public void setApplyPerChannel(boolean applyPerChannel) {
        this.applyPerChannel = applyPerChannel;
    }

    @ACAQDocumentation(name = "Apply per frame", description = "If true, the operation is applied for each frame separately. If false, all frames are put together.")
    @ACAQParameter("apply-per-frame")
    public boolean isApplyPerFrame() {
        return applyPerFrame;
    }

    @ACAQParameter("apply-per-frame")
    public void setApplyPerFrame(boolean applyPerFrame) {
        this.applyPerFrame = applyPerFrame;
    }

    /**
     * Available operations
     */
    public enum Operation {
        LogicalOr,
        LogicalAnd,
        LogicalXor
    }
}
