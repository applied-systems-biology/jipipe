package org.hkijena.acaq5.extension.api.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQSimpleDataSource;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMultichannelImageDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Multichannel image from file")
@AlgorithmOutputSlot(ACAQMultichannelImageDataSlot.class)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQMultichannelImageDataFromFile extends ACAQSimpleDataSource<ACAQMultichannelImageData> {

    private Path fileName;

    public ACAQMultichannelImageDataFromFile(ACAQAlgorithmDeclaration declaration) {
        super("Multichannel Image", declaration, ACAQMultichannelImageDataSlot.class, ACAQMultichannelImageData.class);
    }

    public ACAQMultichannelImageDataFromFile(ACAQMultichannelImageDataFromFile other) {
        super(other);
        this.fileName = other.fileName;
    }

    @Override
    public void run() {
        setOutputData(new ACAQMultichannelImageData(IJ.openImage(fileName.toString())));
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
