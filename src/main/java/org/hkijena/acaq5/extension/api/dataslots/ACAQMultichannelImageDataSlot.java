package org.hkijena.acaq5.extension.api.dataslots;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;

public class ACAQMultichannelImageDataSlot extends ACAQDataSlot<ACAQMultichannelImageData> {
    public ACAQMultichannelImageDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQMultichannelImageData.class);
    }
}
