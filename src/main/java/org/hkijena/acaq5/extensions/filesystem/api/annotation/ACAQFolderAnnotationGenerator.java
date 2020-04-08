package org.hkijena.acaq5.extensions.filesystem.api.annotation;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFolderData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that generates annotations from folder names
 */
@ACAQDocumentation(name = "Folders to annotations", description = "Creates an annotation for each folder based on its name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Annotation)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFolderData.class, slotName = "Annotated folders", autoCreate = true)

// Traits
public class ACAQFolderAnnotationGenerator extends ACAQIteratingAlgorithm {

    private ACAQTraitDeclarationRef generatedAnnotation = new ACAQTraitDeclarationRef();

    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public ACAQFolderAnnotationGenerator(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        updateSlotTraits();
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQFolderAnnotationGenerator(ACAQFolderAnnotationGenerator other) {
        super(other);
        this.generatedAnnotation = new ACAQTraitDeclarationRef(other.generatedAnnotation.getDeclaration());
        updateSlotTraits();
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (generatedAnnotation.getDeclaration() != null) {
            ACAQFolderData inputData = dataInterface.getInputData(getFirstInputSlot());
            String discriminator = inputData.getFolderPath().getFileName().toString();
            dataInterface.addAnnotation(generatedAnnotation.getDeclaration().newInstance(discriminator));
            dataInterface.addOutputData(getFirstOutputSlot(), inputData);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Generated annotation").report(generatedAnnotation);
    }

    private void updateSlotTraits() {
        ACAQDefaultMutableTraitConfiguration traitConfiguration = (ACAQDefaultMutableTraitConfiguration) getTraitConfiguration();
        traitConfiguration.getMutableGlobalTraitModificationTasks().clear();
        if (generatedAnnotation.getDeclaration() != null)
            traitConfiguration.getMutableGlobalTraitModificationTasks().set(generatedAnnotation.getDeclaration(), ACAQTraitModificationOperation.Add);
        traitConfiguration.postChangedEvent();
    }

    /**
     * @return The generated annotation type
     */
    @ACAQDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each folder")
    @ACAQParameter("generated-annotation")
    public ACAQTraitDeclarationRef getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    /**
     * Sets the generated annotation type
     *
     * @param generatedAnnotation The annotation type
     */
    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(ACAQTraitDeclarationRef generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
        updateSlotTraits();
    }
}
