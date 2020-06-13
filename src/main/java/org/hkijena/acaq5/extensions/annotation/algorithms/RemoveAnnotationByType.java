package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.StringList;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Removes a specified annotation
 */
@ACAQDocumentation(name = "Remove annotation by type", description = "Removes annotations of the specified types")
@ACAQOrganization(menuPath = "Remove", algorithmCategory = ACAQAlgorithmCategory.Annotation)
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class RemoveAnnotationByType extends ACAQSimpleIteratingAlgorithm {

    private StringList annotationTypes = new StringList();
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
        for (String annotationType : annotationTypes) {
            dataInterface.removeGlobalAnnotation(annotationType);
        }
        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
    }

    @ACAQDocumentation(name = "Removed annotation", description = "This annotation is removed from each input data")
    @ACAQParameter("annotation-type")
    public StringList getAnnotationTypes() {
        return annotationTypes;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationTypes(StringList annotationTypes) {
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
