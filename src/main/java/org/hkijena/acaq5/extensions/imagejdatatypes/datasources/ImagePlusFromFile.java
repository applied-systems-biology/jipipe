package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loads an image data from a file via IJ.openFile()
 */
@ACAQDocumentation(name = "Import 2D image")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class ImagePlusFromFile extends ACAQSimpleIteratingAlgorithm {

    private Class<? extends ACAQData> dataClass;
    private ACAQTraitDeclarationRef titleAnnotation = new ACAQTraitDeclarationRef(ACAQTraitRegistry.getInstance().getDeclarationById("image-title"));

    /**
     * @param declaration algorithm declaration
     * @param dataClass   loaded data class
     */
    public ImagePlusFromFile(ACAQAlgorithmDeclaration declaration, Class<? extends ACAQData> dataClass) {
        super(declaration,
                ACAQMutableSlotConfiguration.builder().addInputSlot("Files", FileData.class)
                        .addOutputSlot("Image", dataClass, "")
                        .sealOutput()
                        .sealInput()
                        .build());
        this.dataClass = dataClass;
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ImagePlusFromFile(ImagePlusFromFile other) {
        super(other);
        this.dataClass = other.dataClass;
        this.titleAnnotation = new ACAQTraitDeclarationRef(other.titleAnnotation);
    }

    @Override
    protected void initializeTraits() {
        super.initializeTraits();
        ((ACAQDefaultMutableTraitConfiguration) getTraitConfiguration()).setTraitModificationsSealed(false);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData fileData = dataInterface.getInputData(getFirstInputSlot(), FileData.class);
        ImagePlusData data = (ImagePlusData) readImageFrom(fileData.getPath());
        List<ACAQTrait> traits = new ArrayList<>();
        if (titleAnnotation.getDeclaration() != null) {
            traits.add(titleAnnotation.getDeclaration().newInstance(data.getImage().getTitle()));
        }
        dataInterface.addOutputData(getFirstOutputSlot(), data);
    }

    @ACAQDocumentation(name = "Title annotation", description = "Optional annotation type where the image title is written.")
    @ACAQParameter("title-annotation")
    public ACAQTraitDeclarationRef getTitleAnnotation() {
        if (titleAnnotation == null)
            titleAnnotation = new ACAQTraitDeclarationRef();
        return titleAnnotation;
    }

    @ACAQParameter("title-annotation")
    public void setTitleAnnotation(ACAQTraitDeclarationRef titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    /**
     * Loads an image from a file path
     *
     * @param fileName the image file name
     * @return the generated data
     */
    protected ACAQData readImageFrom(Path fileName) {
        try {
            ImagePlus imagePlus = IJ.openImage(fileName.toString());
            imagePlus.getProcessor().setLut(null);
            return dataClass.getConstructor(ImagePlus.class).newInstance(imagePlus);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
