package org.hkijena.acaq5.extension.api.dataslots;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;

public class ACAQROIDataSlot extends ACAQDataSlot<ACAQROIData> {
    public ACAQROIDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQROIData.class);
    }
}
