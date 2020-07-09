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

package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FolderData;
import org.hkijena.acaq5.extensions.filesystem.dataypes.PathData;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that generates annotations from folder names
 */
@ACAQDocumentation(name = "Folders to annotations", description = "Creates an annotation for each path based on its name")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Generate")
@AlgorithmInputSlot(value = PathData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = PathData.class, slotName = "Annotated folders", autoCreate = true)
@ACAQHidden
public class SimpleFolderAnnotationGenerator extends ACAQSimpleIteratingAlgorithm {

    private String generatedAnnotation = "";

    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public SimpleFolderAnnotationGenerator(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public SimpleFolderAnnotationGenerator(SimpleFolderAnnotationGenerator other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
            FolderData inputData = dataInterface.getInputData(getFirstInputSlot(), FolderData.class);
            String discriminator = inputData.getPath().getFileName().toString();
            dataInterface.addGlobalAnnotation(new ACAQAnnotation(generatedAnnotation, discriminator));
            dataInterface.addOutputData(getFirstOutputSlot(), inputData);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Generated annotation").checkNonEmpty(generatedAnnotation, this);
    }

    /**
     * @return The generated annotation type
     */
    @ACAQDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each folder")
    @ACAQParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    /**
     * Sets the generated annotation type
     *
     * @param generatedAnnotation The annotation type
     */
    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }
}
