package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.parameters.editors.ACAQTraitParameterSettings;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Removes a specified annotation
 */
@ACAQDocumentation(name = "Remove annotation", description = "Removes annotations")
@ACAQOrganization(menuPath = "Remove", algorithmCategory = ACAQAlgorithmCategory.Annotation)
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class RemoveAnnotationByType extends ACAQSimpleIteratingAlgorithm {

    private ACAQTraitDeclarationRef.List annotationTypes = new ACAQTraitDeclarationRef.List();
    private boolean removeCategory = true;

    /**
     * @param declaration algorithm declaration
     */
    public RemoveAnnotationByType(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQIOSlotConfiguration());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public RemoveAnnotationByType(RemoveAnnotationByType other) {
        super(other);
        this.annotationTypes = other.annotationTypes;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (ACAQTraitDeclarationRef annotationType : annotationTypes) {
            dataInterface.removeGlobalAnnotation(annotationType.getDeclaration(), removeCategory);
        }
        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
    }

    @ACAQDocumentation(name = "Removed annotation", description = "This annotation is removed from each input data")
    @ACAQParameter("annotation-type")
    @ACAQTraitParameterSettings(showHidden = true)
    public ACAQTraitDeclarationRef.List getAnnotationTypes() {
        return annotationTypes;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationTypes(ACAQTraitDeclarationRef.List annotationTypes) {
        this.annotationTypes = annotationTypes;
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
