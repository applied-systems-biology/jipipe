package org.hkijena.acaq5.api.batchimporter.dataslots;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFilesData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

public class ACAQFilesDataSlot extends ACAQDataSlot<ACAQFilesData> {
    public ACAQFilesDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQFilesData.class);
    }
}
