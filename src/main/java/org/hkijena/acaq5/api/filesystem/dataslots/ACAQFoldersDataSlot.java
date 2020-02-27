package org.hkijena.acaq5.api.filesystem.dataslots;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFoldersData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

public class ACAQFoldersDataSlot extends ACAQDataSlot<ACAQFoldersData> {
    public ACAQFoldersDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQFoldersData.class);
    }
}
