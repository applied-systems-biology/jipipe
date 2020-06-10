package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loads ROI data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "ROI from file")
@AlgorithmInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Mask", autoCreate = true)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class ROIDataFromFile extends ACAQSimpleIteratingAlgorithm {

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
        FileData fileData = dataInterface.getInputData(getFirstInputSlot(), FileData.class);
        List<Roi> rois = ROIListData.loadRoiListFromFile(fileData.getPath());
        dataInterface.addOutputData(getFirstOutputSlot(), new ROIListData(rois));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
