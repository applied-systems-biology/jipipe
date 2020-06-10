package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQIOSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Removes a specified annotation
 */
@ACAQDocumentation(name = "Remove annotation", description = "Removes a specified annotation")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation)

// Traits
public class RemoveAnnotations extends ACAQAlgorithm {

    private ACAQTraitDeclarationRef annotationType;

    /**
     * @param declaration algorithm declaration
     */
    public RemoveAnnotations(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQIOSlotConfiguration());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public RemoveAnnotations(RemoveAnnotations other) {
        super(other);
        this.annotationType = other.annotationType;
        updateSlotTraits();
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
            report.forCategory("Removed annotation").reportIsInvalid("No annotation provided!",
                    "You have to determine which annotation type should be removed.",
                    "Please setup an annotation that is removed from the data.",
                    this);
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
