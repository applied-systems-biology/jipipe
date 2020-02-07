package org.hkijena.acaq5.extension.dataslots;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQROIData;

public class ACAQROIDataSlot extends ACAQDataSlot<ACAQROIData> {
    public ACAQROIDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQROIData.class);
    }
}
