package org.hkijena.acaq5.api.batchimporter.dataslots;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQBatchImporterFileData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

public class ACAQBatchImporterFileDataSlot extends ACAQDataSlot<ACAQBatchImporterFileData> {
    public ACAQBatchImporterFileDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name) {
        super(algorithm, slotType, name, ACAQBatchImporterFileData.class);
    }
}
