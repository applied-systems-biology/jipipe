package org.hkijena.acaq5.extension.api.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFileDataSlot;
import org.hkijena.acaq5.api.filesystem.dataypes.ACAQFileData;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMultichannelImageDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;

import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Multichannel image from file")
@AlgorithmInputSlot(value = ACAQFileDataSlot.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMultichannelImageDataSlot.class, slotName = "Multichannel Image", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQMultichannelImageDataFromFile extends ACAQIteratingAlgorithm {

    public ACAQMultichannelImageDataFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQMultichannelImageDataFromFile(ACAQMultichannelImageDataFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFileData fileData = dataInterface.getInputData(getFirstInputSlot());
        dataInterface.addOutputData(getFirstOutputSlot(), readImageFrom(fileData.getFilePath()));
    }

    protected ACAQData readImageFrom(Path fileName) {
        return new ACAQMultichannelImageData(IJ.openImage(fileName.toString()));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
