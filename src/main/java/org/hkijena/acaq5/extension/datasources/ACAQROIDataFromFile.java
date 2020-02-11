package org.hkijena.acaq5.extension.datasources;

import ij.IJ;
import ij.io.RoiDecoder;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.datatypes.ACAQROIData;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads ROI data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "ROI from file")
@ACAQGenerates(ACAQROIData.class)
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQROIDataFromFile extends ACAQSimpleDataSource<ACAQROIData> {

    private Path fileName;

    public ACAQROIDataFromFile() {
        super("Mask", ACAQROIDataSlot.class, ACAQROIData.class);
    }

    @Override
    public void run() {
        RoiDecoder decoder = new RoiDecoder(fileName.toString());
        try {
            setOutputData(new ACAQROIData(decoder.getRoi()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
