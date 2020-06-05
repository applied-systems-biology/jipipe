package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Appends the ROI into one list
 */
@ACAQOrganization(menuPath = "Merge", algorithmCategory = ACAQAlgorithmCategory.Processor)
@ACAQDocumentation(name = "Merge ROI (deprecated)", description = "Appends the ROI into one list")

// Data flow
@AlgorithmInputSlot(ROIListData.class)
@AlgorithmOutputSlot(ROIListData.class)

// Traits
public class MergeROIEnhancer extends ACAQIteratingAlgorithm {
    /**
     * @param declaration the algorithm declaration
     */
    public MergeROIEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().restrictInputTo(ROIListData.class)
                .addOutputSlot("ROI", ROIListData.class, "")
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
            ROIListData data = dataInterface.getInputData(slot, ROIListData.class);
            inputROI.addAll(data);
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ROIListData(inputROI));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
