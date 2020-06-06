package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;

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
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData data = (ROIListData) dataInterface.getInputData(getFirstInputSlot(), ROIListData.class).duplicate();
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
        if(splitAfterwards)
            data.splitAll();
        dataInterface.addOutputData(getFirstOutputSlot(), data);
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

    /**
     * Available operations
     */
    public enum Operation {
        LogicalOr,
        LogicalAnd,
        LogicalXor
    }
}
