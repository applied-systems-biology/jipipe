package org.hkijena.acaq5.api.compartments.dataslots;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.compartments.datatypes.ACAQPreprocessingOutputData;

public class ACAQPreprocessingOutputDataSlot extends ACAQDataSlot<ACAQPreprocessingOutputData> {
    public ACAQPreprocessingOutputDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQPreprocessingOutputData.class);
    }
}
