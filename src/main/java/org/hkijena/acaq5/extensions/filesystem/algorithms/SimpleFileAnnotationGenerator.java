package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

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

    private ACAQTraitDeclarationRef generatedAnnotation = new ACAQTraitDeclarationRef();

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
        this.generatedAnnotation = new ACAQTraitDeclarationRef(other.generatedAnnotation.getDeclaration());
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (generatedAnnotation.getDeclaration() != null) {
            FileData inputData = dataInterface.getInputData(getFirstInputSlot(), FileData.class);
            String discriminator = inputData.getPath().getFileName().toString();
            dataInterface.addGlobalAnnotation(generatedAnnotation.getDeclaration().newInstance(discriminator));
            dataInterface.addOutputData(getFirstOutputSlot(), inputData);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Generated annotation").report(generatedAnnotation);
    }

    /**
     * @return Generated annotation type
     */
    @ACAQDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each file")
    @ACAQParameter("generated-annotation")
    public ACAQTraitDeclarationRef getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    /**
     * Sets generated annotation type
     *
     * @param generatedAnnotation Annotation type
     */
    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(ACAQTraitDeclarationRef generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }
}
