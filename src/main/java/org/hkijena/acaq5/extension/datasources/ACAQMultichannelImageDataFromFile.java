package org.hkijena.acaq5.extension.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQMultichannelImageDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMultichannelImageData;

import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Multichannel image from file")
@ACAQGenerates(ACAQMultichannelImageData.class)
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQMultichannelImageDataFromFile extends ACAQSimpleDataSource<ACAQMultichannelImageData> {

    private Path fileName;

    public ACAQMultichannelImageDataFromFile() {
        super("Multichannel Image", ACAQMultichannelImageDataSlot.class, ACAQMultichannelImageData.class);
    }

    @Override
    public void run() {
        setOutputData(new ACAQMultichannelImageData(IJ.openImage(fileName.toString())));
    }
}
