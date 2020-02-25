package org.hkijena.acaq5.extension.api.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQSimpleDataSource;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Greyscale image from file")
@AlgorithmOutputSlot(value = ACAQGreyscaleImageDataSlot.class, slotName = "Greyscale image", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource, visibility = ACAQAlgorithmVisibility.PreprocessingAnalysisOnly)
public class ACAQGreyscaleImageDataFromFile extends ACAQSimpleDataSource<ACAQGreyscaleImageData> {

    private Path fileName;

    public ACAQGreyscaleImageDataFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQGreyscaleImageDataFromFile(ACAQGreyscaleImageDataFromFile other) {
        super(other);
        this.fileName = other.fileName;
    }

    @Override
    public void run() {
        setOutputData(new ACAQGreyscaleImageData(IJ.openImage(fileName.toString())));
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
