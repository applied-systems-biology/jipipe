package org.hkijena.acaq5.extension.api.datasources;

import ij.io.RoiDecoder;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.api.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;

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
