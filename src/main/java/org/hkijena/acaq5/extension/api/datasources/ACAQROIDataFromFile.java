package org.hkijena.acaq5.extension.api.datasources;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFileDataSlot;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFileData;
import org.hkijena.acaq5.extension.api.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;

import java.util.List;

/**
 * Loads ROI data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "ROI from file")
@AlgorithmInputSlot(value = ACAQFileDataSlot.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQROIDataSlot.class, slotName = "Mask", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
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
