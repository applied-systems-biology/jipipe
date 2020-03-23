package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

/**
 * Loads an image data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Import 2D image")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ImagePlusFromFile extends ACAQIteratingAlgorithm {

    private Class<? extends ACAQData> dataClass;

    public ImagePlusFromFile(ACAQAlgorithmDeclaration declaration, Class<? extends ACAQData> dataClass) {
        super(declaration,
                ACAQMutableSlotConfiguration.builder().addInputSlot("Files", ACAQFileData.class)
                        .addOutputSlot("Image", "", dataClass)
                        .sealOutput()
                        .sealInput()
                        .build());
        this.dataClass = dataClass;
    }

    public ImagePlusFromFile(ImagePlusFromFile other) {
        super(other);
        this.dataClass = other.dataClass;
    }

    @Override
    protected void initializeTraits() {
        super.initializeTraits();
        ((ACAQDefaultMutableTraitConfiguration) getTraitConfiguration()).setTraitModificationsSealed(false);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFileData fileData = dataInterface.getInputData(getFirstInputSlot());
        dataInterface.addOutputData(getFirstOutputSlot(), readImageFrom(fileData.getFilePath()));
    }

    protected ACAQData readImageFrom(Path fileName) {
        try {
            return dataClass.getConstructor(ImagePlus.class).newInstance(IJ.openImage(fileName.toString()));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
