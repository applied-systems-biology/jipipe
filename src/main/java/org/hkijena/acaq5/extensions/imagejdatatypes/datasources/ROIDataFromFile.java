package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FileData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIData;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loads ROI data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "ROI from file")
@AlgorithmInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ROIData.class, slotName = "Mask", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ROIDataFromFile extends ACAQIteratingAlgorithm {

    /**
     * @param declaration the algorithm declaration
     */
    public ROIDataFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ROIDataFromFile(ROIDataFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData fileData = dataInterface.getInputData(getFirstInputSlot());
        List<Roi> rois = ROIData.loadRoiListFromFile(fileData.getFilePath());
        dataInterface.addOutputData(getFirstOutputSlot(), new ROIData(rois));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
