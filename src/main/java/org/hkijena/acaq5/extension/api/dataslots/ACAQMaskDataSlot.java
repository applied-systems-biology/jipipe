package org.hkijena.acaq5.extension.api.dataslots;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;

public class ACAQMaskDataSlot extends ACAQDataSlot<ACAQMaskData> {
    public ACAQMaskDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQMaskData.class);
    }
}
