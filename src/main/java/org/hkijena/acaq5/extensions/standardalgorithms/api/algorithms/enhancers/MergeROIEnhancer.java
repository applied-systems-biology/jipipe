package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIData;

import java.util.ArrayList;
import java.util.List;

@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)
@ACAQOrganization(menuPath = "Merge")
@ACAQDocumentation(name = "Merge ROI")

// Data flow
@AlgorithmInputSlot(ROIData.class)
@AlgorithmOutputSlot(ROIData.class)

// Traits
public class MergeROIEnhancer extends ACAQIteratingAlgorithm {
    public MergeROIEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().restrictInputTo(ROIData.class)
                .addOutputSlot("ROI", "", ROIData.class)
                .sealOutput().build(), null);
    }

    public MergeROIEnhancer(MergeROIEnhancer other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
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
