package org.hkijena.acaq5.extension.api.datasources;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.traits.global.AutoTransferTraits;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;

import java.util.List;

/**
 * Loads ROI data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "ROI from file")
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQROIData.class, slotName = "Mask", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
@AutoTransferTraits
public class ACAQROIDataFromFile extends ACAQIteratingAlgorithm {

    public ACAQROIDataFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQROIDataFromFile(ACAQROIDataFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFileData fileData = dataInterface.getInputData(getFirstInputSlot());
        List<Roi> rois = ACAQROIData.loadRoiListFromFile(fileData.getFilePath());
        dataInterface.addOutputData(getFirstOutputSlot(), new ACAQROIData(rois));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
