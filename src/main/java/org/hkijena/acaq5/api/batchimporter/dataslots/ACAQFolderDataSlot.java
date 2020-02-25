package org.hkijena.acaq5.api.batchimporter.dataslots;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFileData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

public class ACAQFolderDataSlot extends ACAQDataSlot<ACAQFolderData> {
    public ACAQFolderDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQFolderData.class);
    }
}
