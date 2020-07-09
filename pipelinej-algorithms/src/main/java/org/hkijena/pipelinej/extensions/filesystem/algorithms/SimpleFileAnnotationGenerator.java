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

package org.hkijena.pipelinej.extensions.filesystem.algorithms;

import org.hkijena.pipelinej.api.*;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQAnnotation;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.filesystem.dataypes.FileData;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.pipelinej.utils.ResourceUtils;
import org.hkijena.pipelinej.utils.StringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates annotations from filenames
 */
@ACAQDocumentation(name = "Files to annotations", description = "Creates an annotation for each file based on its file name")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Generate")
@AlgorithmInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = FileData.class, slotName = "Annotated files", autoCreate = true)
@ACAQHidden
public class SimpleFileAnnotationGenerator extends ACAQSimpleIteratingAlgorithm {

    private String generatedAnnotation = "Dataset";

    /**
     * New instance
     *
     * @param declaration Algorithm declaration
     */
    public SimpleFileAnnotationGenerator(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public SimpleFileAnnotationGenerator(SimpleFileAnnotationGenerator other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
            FileData inputData = dataInterface.getInputData(getFirstInputSlot(), FileData.class);
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
     * @return Generated annotation type
     */
    @ACAQDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each file")
    @ACAQParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    /**
     * Sets generated annotation type
     *
     * @param generatedAnnotation Annotation type
     */
    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }
}
