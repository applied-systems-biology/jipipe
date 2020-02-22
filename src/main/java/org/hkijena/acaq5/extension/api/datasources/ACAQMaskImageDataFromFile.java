package org.hkijena.acaq5.extension.api.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQSimpleDataSource;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Mask from file")
@AlgorithmOutputSlot(ACAQMaskDataSlot.class)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQMaskImageDataFromFile extends ACAQSimpleDataSource<ACAQMaskData> {

    private Path fileName;

    public ACAQMaskImageDataFromFile() {
        super("Mask", ACAQMaskDataSlot.class, ACAQMaskData.class);
    }

    public ACAQMaskImageDataFromFile(ACAQMaskImageDataFromFile other) {
        super(other);
        this.fileName = other.fileName;
    }

    @Override
    public void run() {
        setOutputData(new ACAQMaskData(IJ.openImage(fileName.toString())));
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
