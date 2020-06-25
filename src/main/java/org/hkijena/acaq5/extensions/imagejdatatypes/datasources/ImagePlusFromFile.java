/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

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
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;

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
    private OptionalStringParameter titleAnnotation = new OptionalStringParameter();

    /**
     * @param declaration algorithm declaration
     * @param dataClass   loaded data class
     */
    public ImagePlusFromFile(ACAQAlgorithmDeclaration declaration, Class<? extends ACAQData> dataClass) {
        super(declaration,
                ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Files", FileData.class)
                        .addOutputSlot("Image", dataClass, "")
                        .sealOutput()
                        .sealInput()
                        .build());
        this.dataClass = dataClass;
        titleAnnotation.setContent("Image title");
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ImagePlusFromFile(ImagePlusFromFile other) {
        super(other);
        this.dataClass = other.dataClass;
        this.titleAnnotation = new OptionalStringParameter(other.titleAnnotation);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData fileData = dataInterface.getInputData(getFirstInputSlot(), FileData.class);
        ImagePlusData data = (ImagePlusData) readImageFrom(fileData.getPath());
        List<ACAQAnnotation> traits = new ArrayList<>();
        if (titleAnnotation.isEnabled()) {
            traits.add(new ACAQAnnotation(titleAnnotation.getContent(), data.getImage().getTitle()));
        }
        dataInterface.addOutputData(getFirstOutputSlot(), data, traits);
    }

    @ACAQDocumentation(name = "Title annotation", description = "Optional annotation type where the image title is written.")
    @ACAQParameter("title-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @ACAQParameter("title-annotation")
    public void setTitleAnnotation(OptionalStringParameter titleAnnotation) {
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
        if (titleAnnotation.isEnabled()) {
            report.forCategory("Title annotation").checkNonEmpty(titleAnnotation.getContent(), this);
        }
    }
}
