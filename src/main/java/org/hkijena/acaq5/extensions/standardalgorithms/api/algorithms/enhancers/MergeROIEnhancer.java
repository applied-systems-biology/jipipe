package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Appends the ROI into one list
 */
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)
@ACAQOrganization(menuPath = "Merge")
@ACAQDocumentation(name = "Merge ROI", description = "Appends the ROI into one list")

// Data flow
@AlgorithmInputSlot(ROIData.class)
@AlgorithmOutputSlot(ROIData.class)

// Traits
public class MergeROIEnhancer extends ACAQIteratingAlgorithm {
    /**
     * @param declaration the algorithm declaration
     */
    public MergeROIEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().restrictInputTo(ROIData.class)
                .addOutputSlot("ROI", "", ROIData.class)
                .sealOutput().build(), null);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public MergeROIEnhancer(MergeROIEnhancer other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        List<Roi> inputROI = new ArrayList<>();
        for (ACAQDataSlot slot : getInputSlots()) {
            ROIData data = dataInterface.getInputData(slot);
            inputROI.addAll(data.getROI());
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ROIData(inputROI));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
