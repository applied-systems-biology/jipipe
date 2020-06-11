package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.parameters.editors.ACAQTraitParameterSettings;
import org.hkijena.acaq5.extensions.parameters.pairs.ACAQTraitDeclarationRefAndStringPredicatePair;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Removes a specified annotation
 */
@ACAQDocumentation(name = "Remove annotation by value", description = "Removes annotations that match a filter value")
@ACAQOrganization(menuPath = "Remove", algorithmCategory = ACAQAlgorithmCategory.Annotation)
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class RemoveAnnotationByValue extends ACAQSimpleIteratingAlgorithm {

    private ACAQTraitDeclarationRefAndStringPredicatePair.List filters = new ACAQTraitDeclarationRefAndStringPredicatePair.List();
    private boolean removeCategory = true;

    /**
     * @param declaration algorithm declaration
     */
    public RemoveAnnotationByValue(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQIOSlotConfiguration());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public RemoveAnnotationByValue(RemoveAnnotationByValue other) {
        super(other);
        this.filters = other.filters;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (ACAQTraitDeclarationRefAndStringPredicatePair filter : filters) {
            ACAQTrait instance = dataInterface.getAnnotationOfType(filter.getKey().getDeclaration());
            if (instance != null) {
                if (filter.getValue().test(instance.getValue())) {
                    dataInterface.removeGlobalAnnotation(filter.getKey().getDeclaration(), removeCategory);
                }
            }
        }
        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
    }

    @ACAQDocumentation(name = "Removed annotation", description = "This annotation is removed from each input data")
    @ACAQParameter("filters")
    @ACAQTraitParameterSettings(showHidden = true)
    public ACAQTraitDeclarationRefAndStringPredicatePair.List getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(ACAQTraitDeclarationRefAndStringPredicatePair.List annotationTypes) {
        this.filters = annotationTypes;
        getEventBus().post(new ParameterChangedEvent(this, "filters"));
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
