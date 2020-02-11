package org.hkijena.acaq5.extension.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;

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

    @Override
    public void run() {
        setOutputData(new ACAQMaskData(IJ.openImage(fileName.toString())));
    }
}
