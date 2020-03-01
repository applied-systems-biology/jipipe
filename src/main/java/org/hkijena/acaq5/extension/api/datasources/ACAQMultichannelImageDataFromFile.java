package org.hkijena.acaq5.extension.api.datasources;

import ij.IJ;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.traits.global.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.traits.global.ACAQMutableTraitConfiguration;
import org.hkijena.acaq5.api.traits.global.AutoTransferTraits;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;

import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Multichannel image from file")
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMultichannelImageData.class, slotName = "Multichannel Image", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
@AutoTransferTraits
public class ACAQMultichannelImageDataFromFile extends ACAQIteratingAlgorithm {

    public ACAQMultichannelImageDataFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQMultichannelImageDataFromFile(ACAQMultichannelImageDataFromFile other) {
        super(other);
    }

    @Override
    protected void initializeTraits() {
        super.initializeTraits();
        ((ACAQDefaultMutableTraitConfiguration)getTraitConfiguration()).setTraitModificationsSealed(false);
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
