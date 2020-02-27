package org.hkijena.acaq5.api.filesystem.dataslots;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFileData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

public class ACAQFileDataSlot extends ACAQDataSlot<ACAQFileData> {
    public ACAQFileDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQFileData.class);
    }
}
