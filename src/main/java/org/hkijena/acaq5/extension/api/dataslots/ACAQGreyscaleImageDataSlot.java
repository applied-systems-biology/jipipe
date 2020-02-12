package org.hkijena.acaq5.extension.api.dataslots;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;

public class ACAQGreyscaleImageDataSlot extends ACAQDataSlot<ACAQGreyscaleImageData> {
    public ACAQGreyscaleImageDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQGreyscaleImageData.class);
    }
}
