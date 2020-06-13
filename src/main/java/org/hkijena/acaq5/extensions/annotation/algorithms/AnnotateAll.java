package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.acaq5.extensions.parameters.pairs.StringAndStringPair;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that annotates all data with the same annotation
 */
@ACAQDocumentation(name = "Set annotations", description = "Sets the specified annotations to the specified values")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Modify")
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class AnnotateAll extends ACAQSimpleIteratingAlgorithm {

    private StringAndStringPair.List annotations = new StringAndStringPair.List();
    private boolean overwrite = false;

    /**
     * @param declaration the declaration
     */
    public AnnotateAll(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        annotations.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AnnotateAll(AnnotateAll other) {
        super(other);
        this.annotations = new StringAndStringPair.List(other.annotations);
        this.overwrite = other.overwrite;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (int i = 0; i < annotations.size(); i++) {
            report.forCategory("Annotations").forCategory("Item #" + (i + 1)).forCategory("Name").checkNonEmpty(annotations.get(i).getKey(), this);
        }
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (StringAndStringPair annotation : annotations) {
            dataInterface.addGlobalAnnotation(new ACAQAnnotation(annotation.getKey(), annotation.getValue()), overwrite);
        }
        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
    }

    @ACAQDocumentation(name = "Annotations", description = "Allows you to set the annotation to add/modify")
    @ACAQParameter("generated-annotation")
    @PairParameterSettings(keyLabel = "Name", valueLabel = "Value")
    @StringParameterSettings(monospace = true)
    public StringAndStringPair.List getAnnotations() {
        return annotations;
    }

    @ACAQParameter("generated-annotation")
    public void setAnnotations(StringAndStringPair.List annotations) {
        this.annotations = annotations;
        getEventBus().post(new ParameterChangedEvent(this, "generated-annotation"));
    }

    @ACAQDocumentation(name = "Overwrite existing annotations", description = "If disabled, any existing annotation of the same type (not value) is not replaced")
    @ACAQParameter("overwrite")
    public boolean isOverwrite() {
        return overwrite;
    }

    @ACAQParameter("overwrite")
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        getEventBus().post(new ParameterChangedEvent(this, "overwrite"));
    }
}
