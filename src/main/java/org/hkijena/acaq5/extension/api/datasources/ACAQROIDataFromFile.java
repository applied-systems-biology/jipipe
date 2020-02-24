package org.hkijena.acaq5.extension.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQSimpleDataSource;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads ROI data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "ROI from file")
@AlgorithmOutputSlot(ACAQROIDataSlot.class)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQROIDataFromFile extends ACAQSimpleDataSource<ACAQROIData> {

    private Path fileName;

    public ACAQROIDataFromFile() {
        super("Mask", ACAQROIDataSlot.class, ACAQROIData.class);
    }

    public ACAQROIDataFromFile(ACAQROIDataFromFile other) {
        super(other);
        this.fileName = other.fileName;
    }

    @Override
    public void run() {
        setOutputData(new ACAQROIData(ACAQROIData.loadRoiListFromFile(fileName)));
    }

    @ACAQParameter("file-name")
    public void setFileName(Path fileName) {
        this.fileName = fileName;
    }

    @ACAQParameter("file-name")
    @ACAQDocumentation(name = "File name")
    public Path getFileName() {
        return fileName;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if(fileName == null ||!Files.isRegularFile(fileName))
            report.reportIsInvalid("Input file does not exist! Please provide a valid input file.");
    }
}
