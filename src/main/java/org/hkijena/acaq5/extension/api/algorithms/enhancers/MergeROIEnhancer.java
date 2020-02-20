package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.gui.Roi;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmMetadata;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.extension.api.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;

import java.util.ArrayList;
import java.util.List;

@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)
@ACAQDocumentation(name = "Merge ROI")
@AutoTransferTraits
public class MergeROIEnhancer extends ACAQAlgorithm {
    public MergeROIEnhancer() {
        super(ACAQMutableSlotConfiguration.builder().restrictInputTo(ACAQROIDataSlot.class)
        .addOutputSlot("ROI", ACAQROIDataSlot.class)
        .sealOutput().build());
    }

    public MergeROIEnhancer(MergeROIEnhancer other) {
        super(other);
    }

    @Override
    public void run() {
        List<Roi> inputROI = new ArrayList<>();
        for(ACAQDataSlot<?> slot : getInputSlots()) {
            inputROI.addAll(((ACAQROIDataSlot)slot).getData().getROI());
        }

        ACAQROIDataSlot outputSlot = (ACAQROIDataSlot)getOutputSlots().get(0);
        outputSlot.setData(new ACAQROIData(inputROI));
    }
}
