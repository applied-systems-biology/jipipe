package org.hkijena.acaq5.extension.api.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmMetadata;
import org.hkijena.acaq5.api.data.ACAQGeneratesData;
import org.hkijena.acaq5.api.data.ACAQSimpleDataSource;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;

import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Greyscale image from file")
@ACAQGeneratesData(ACAQGreyscaleImageData.class)
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQGreyscaleImageDataFromFile extends ACAQSimpleDataSource<ACAQGreyscaleImageData> {

    private Path fileName;

    public ACAQGreyscaleImageDataFromFile() {
        super("Greyscale Image", ACAQGreyscaleImageDataSlot.class, ACAQGreyscaleImageData.class);
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
}
