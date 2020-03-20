package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQROIData;

import java.util.List;

/**
 * Loads ROI data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "ROI from file")
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQROIData.class, slotName = "Mask", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ROIDataFromFile extends ACAQIteratingAlgorithm {

    public ROIDataFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ROIDataFromFile(ROIDataFromFile other) {
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
