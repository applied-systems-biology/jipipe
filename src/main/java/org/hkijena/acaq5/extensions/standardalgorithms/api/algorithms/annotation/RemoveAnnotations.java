package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.annotation;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;

@ACAQDocumentation(name = "Remove annotation", description = "Removes a specified annotation")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Annotation)

// Traits
public class RemoveAnnotations extends ACAQAlgorithm {

    private ACAQTraitDeclarationRef annotationType;

    public RemoveAnnotations(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQIOSlotConfiguration());
    }

    public RemoveAnnotations(RemoveAnnotations other) {
        super(other);
        this.annotationType = other.annotationType;
        updateSlotTraits();
    }

    @Override
    public void run() {
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            ACAQDataSlot outputSlot = getSlots().get("Output " + inputSlot.getName());
            outputSlot.copyFrom(inputSlot);
        }
        for (ACAQDataSlot outputSlot : getOutputSlots()) {
            outputSlot.removeAllAnnotationsFromData(annotationType.getDeclaration());
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (annotationType.getDeclaration() == null) {
            report.forCategory("Removed annotation").reportIsInvalid("No annotation provided! Please setup an annotation that is removed from the data.");
        }
    }

    private void updateSlotTraits() {
        ACAQDefaultMutableTraitConfiguration traitConfiguration = (ACAQDefaultMutableTraitConfiguration) getTraitConfiguration();
        traitConfiguration.getMutableGlobalTraitModificationTasks().clear();
        if (annotationType != null)
            traitConfiguration.getMutableGlobalTraitModificationTasks().set(annotationType.getDeclaration(), ACAQTraitModificationOperation.RemoveThis);
        traitConfiguration.postChangedEvent();
    }

    @ACAQDocumentation(name = "Removed annotation", description = "This annotation is removed from each input data")
    @ACAQParameter("annotation-type")
    public ACAQTraitDeclarationRef getAnnotationType() {
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(ACAQTraitDeclarationRef annotationType) {
        this.annotationType = annotationType;
        updateSlotTraits();
        getEventBus().post(new ParameterChangedEvent(this, "annotation-type"));
    }

}
