package org.hkijena.acaq5.extension.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFileDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;

import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Mask from file")
@AlgorithmInputSlot(value = ACAQFileDataSlot.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMaskDataSlot.class, slotName = "Mask", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQMaskImageDataFromFile extends ACAQMultichannelImageDataFromFile {

    public ACAQMaskImageDataFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQMaskImageDataFromFile(ACAQMaskImageDataFromFile other) {
        super(other);
    }

    @Override
    protected ACAQData readImageFrom(Path fileName) {
        ACAQMultichannelImageData multichannelImageData = (ACAQMultichannelImageData) super.readImageFrom(fileName);
        return new ACAQMaskData(multichannelImageData.getImage());
    }
}
