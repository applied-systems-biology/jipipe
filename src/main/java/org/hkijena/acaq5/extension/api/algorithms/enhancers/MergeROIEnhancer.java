package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;

import java.util.ArrayList;
import java.util.List;

@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)
@ACAQDocumentation(name = "Merge ROI")

// Data flow
@AlgorithmInputSlot(ACAQROIData.class)
@AlgorithmOutputSlot(ACAQROIData.class)

// Traits
@AutoTransferTraits
public class MergeROIEnhancer extends ACAQIteratingAlgorithm {
    public MergeROIEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().restrictInputTo(ACAQROIData.class)
                .addOutputSlot("ROI", ACAQROIData.class)
                .sealOutput().build(), null);
    }

    public MergeROIEnhancer(MergeROIEnhancer other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        List<Roi> inputROI = new ArrayList<>();
        for (ACAQDataSlot slot : getInputSlots()) {
            ACAQROIData data = dataInterface.getInputData(slot);
            inputROI.addAll(data.getROI());
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ACAQROIData(inputROI));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
