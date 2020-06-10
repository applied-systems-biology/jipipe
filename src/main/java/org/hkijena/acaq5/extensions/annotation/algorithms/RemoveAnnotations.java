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
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefList;
import org.hkijena.acaq5.extensions.parameters.editors.ACAQTraitParameterSettings;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Removes a specified annotation
 */
@ACAQDocumentation(name = "Remove annotation", description = "Removes annotations")
@ACAQOrganization(menuPath = "Remove", algorithmCategory = ACAQAlgorithmCategory.Annotation)

// Traits
public class RemoveAnnotations extends ACAQAlgorithm {

    private ACAQTraitDeclarationRefList annotationTypes = new ACAQTraitDeclarationRefList();
    private boolean removeCategory = true;

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
        this.annotationTypes = other.annotationTypes;
        updateSlotTraits();
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            ACAQDataSlot outputSlot = getSlots().get("Output " + inputSlot.getName());
            outputSlot.copyFrom(inputSlot);
        }
        for (ACAQDataSlot outputSlot : getOutputSlots()) {
            for (ACAQTraitDeclarationRef annotationType : annotationTypes) {
                outputSlot.removeAllAnnotationsFromData(annotationType.getDeclaration());
                if(removeCategory) {
                    for (ACAQTraitDeclaration inherited : annotationType.getDeclaration().getInherited()) {
                        outputSlot.removeAllAnnotationsFromData(inherited);
                    }
                }
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    private void updateSlotTraits() {
        ACAQDefaultMutableTraitConfiguration traitConfiguration = (ACAQDefaultMutableTraitConfiguration) getTraitConfiguration();
        traitConfiguration.getMutableGlobalTraitModificationTasks().clear();
        if (annotationTypes != null) {
            for (ACAQTraitDeclarationRef annotationType : annotationTypes) {
                if (removeCategory) {
                    traitConfiguration.getMutableGlobalTraitModificationTasks().set(annotationType.getDeclaration(),
                            ACAQTraitModificationOperation.RemoveCategory);
                } else {
                    traitConfiguration.getMutableGlobalTraitModificationTasks().set(annotationType.getDeclaration(),
                            ACAQTraitModificationOperation.RemoveThis);
                }
            }
        }
        traitConfiguration.postChangedEvent();
    }

    @ACAQDocumentation(name = "Removed annotation", description = "This annotation is removed from each input data")
    @ACAQParameter("annotation-type")
    @ACAQTraitParameterSettings(showHidden = true)
    public ACAQTraitDeclarationRefList getAnnotationTypes() {
        return annotationTypes;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationTypes(ACAQTraitDeclarationRefList annotationTypes) {
        this.annotationTypes = annotationTypes;
        updateSlotTraits();
        getEventBus().post(new ParameterChangedEvent(this, "annotation-type"));
    }

    @ACAQDocumentation(name = "Remove child annotations", description = "If enabled, annotations that inherit from the selected annotation types " +
            "are removed")
    @ACAQParameter("remove-category")
    public boolean isRemoveCategory() {
        return removeCategory;
    }

    @ACAQParameter("remove-category")
    public void setRemoveCategory(boolean removeCategory) {
        this.removeCategory = removeCategory;
        getEventBus().post(new ParameterChangedEvent(this, "remove-category"));
    }
}
