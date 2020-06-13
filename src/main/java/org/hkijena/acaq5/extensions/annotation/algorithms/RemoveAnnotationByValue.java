package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.extensions.parameters.pairs.StringAndStringPredicatePair;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;

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

    private StringAndStringPredicatePair.List filters = new StringAndStringPredicatePair.List();

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
        for (StringAndStringPredicatePair filter : filters) {
            ACAQAnnotation instance = dataInterface.getAnnotationOfType(filter.getKey());
            if (instance != null) {
                if (filter.getValue().test(instance.getValue())) {
                    dataInterface.removeGlobalAnnotation(filter.getKey());
                }
            }
        }
        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
    }

    @ACAQDocumentation(name = "Removed annotation", description = "This annotation is removed from each input data")
    @ACAQParameter("filters")
    @StringParameterSettings(monospace = true)
    public StringAndStringPredicatePair.List getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(StringAndStringPredicatePair.List annotationTypes) {
        this.filters = annotationTypes;
        getEventBus().post(new ParameterChangedEvent(this, "filters"));
    }

}
