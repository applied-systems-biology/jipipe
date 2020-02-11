package org.hkijena.acaq5.extension.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;

import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Greyscale image from file")
@ACAQGenerates(ACAQGreyscaleImageData.class)
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQGreyscaleImageDataFromFile extends ACAQSimpleDataSource<ACAQGreyscaleImageData> {

    private Path fileName;

    public ACAQGreyscaleImageDataFromFile() {
        super("Greyscale Image", ACAQGreyscaleImageDataSlot.class, ACAQGreyscaleImageData.class);
    }

    @Override
    public void run() {
        setOutputData(new ACAQGreyscaleImageData(IJ.openImage(fileName.toString())));
    }
}
