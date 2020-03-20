package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQMultichannelImageData;

import java.nio.file.Path;

/**
 * Loads greyscale data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Mask from file")
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMaskData.class, slotName = "Mask", autoCreate = true)
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
