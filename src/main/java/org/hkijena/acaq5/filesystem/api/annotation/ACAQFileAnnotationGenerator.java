package org.hkijena.acaq5.filesystem.api.annotation;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.data.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.extension.ui.parametereditors.ACAQTraitDeclarationRefParameterSettings;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFolderData;

@ACAQDocumentation(name = "Files to annotations", description = "Creates an annotation for each file based on its file name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Annotation)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFileData.class, slotName = "Annotated files", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQFileAnnotationGenerator extends ACAQIteratingAlgorithm {

    private ACAQTraitDeclarationRef generatedAnnotation = new ACAQTraitDeclarationRef();

    public ACAQFileAnnotationGenerator(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        updateSlotTraits();
    }

    public ACAQFileAnnotationGenerator(ACAQFileAnnotationGenerator other) {
        super(other);
        this.generatedAnnotation = new ACAQTraitDeclarationRef(other.generatedAnnotation.getDeclaration());
        updateSlotTraits();
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        if (generatedAnnotation.getDeclaration() != null) {
            ACAQFileData inputData = dataInterface.getInputData(getFirstInputSlot());
            String discriminator = inputData.getFilePath().getFileName().toString();
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

    @ACAQDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each file")
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
