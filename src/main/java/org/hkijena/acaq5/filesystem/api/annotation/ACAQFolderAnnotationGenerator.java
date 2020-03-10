package org.hkijena.acaq5.filesystem.api.annotation;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.extension.ui.parametereditors.ACAQTraitDeclarationRefParameterSettings;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFolderData;

@ACAQDocumentation(name = "Folders to annotations", description = "Creates an annotation for each folder based on its name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Annotation)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFolderData.class, slotName = "Annotated folders", autoCreate = true)

// Traits
public class ACAQFolderAnnotationGenerator extends ACAQIteratingAlgorithm {

    private ACAQTraitDeclarationRef generatedAnnotation = new ACAQTraitDeclarationRef();

    public ACAQFolderAnnotationGenerator(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        updateSlotTraits();
    }

    public ACAQFolderAnnotationGenerator(ACAQFolderAnnotationGenerator other) {
        super(other);
        this.generatedAnnotation = new ACAQTraitDeclarationRef(other.generatedAnnotation.getDeclaration());
        updateSlotTraits();
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
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

    @ACAQDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each folder")
    @ACAQTraitDeclarationRefParameterSettings(traitBaseClass = ACAQDiscriminator.class)
    @ACAQParameter("generated-annotation")
    public ACAQTraitDeclarationRef getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(ACAQTraitDeclarationRef generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
        updateSlotTraits();
    }
}
