package org.hkijena.acaq5.extension.api.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;

import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Mask from file")
@ACAQGenerates(ACAQMaskData.class)
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
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
}
