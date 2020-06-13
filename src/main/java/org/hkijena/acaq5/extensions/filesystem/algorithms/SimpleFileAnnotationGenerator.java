package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;

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
