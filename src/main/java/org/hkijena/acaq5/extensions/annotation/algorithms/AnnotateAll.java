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
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQTrait;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that annotates all data with the same annotation
 */
@ACAQDocumentation(name = "Annotate data", description = "Annotates each data with the specified annotation")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation)

// Traits
public class AnnotateAll extends ACAQAlgorithm {

    private ACAQTrait annotation;
    private boolean overwrite = false;

    /**
     * @param declaration the declaration
     */
    public AnnotateAll(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQIOSlotConfiguration());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AnnotateAll(AnnotateAll other) {
        super(other);
        this.annotation = other.annotation;
        this.overwrite = other.overwrite;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            ACAQDataSlot outputSlot = getSlots().get("Output " + inputSlot.getName());
            outputSlot.copyFrom(inputSlot);
        }
        for (ACAQDataSlot outputSlot : getOutputSlots()) {
            outputSlot.addAnnotationToAllData(annotation, overwrite);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (annotation == null) {
            report.forCategory("Annotation").reportIsInvalid("No annotation provided!",
                    "You have to define which annotation should be added to the data.",
                    "Please setup an annotation that is added to the data.",
                    this);
        }
    }

    @ACAQDocumentation(name = "Annotation", description = "This annotation is added to each input data")
    @ACAQParameter("generated-annotation")
    public ACAQTrait getAnnotation() {
        return annotation;
    }

    @ACAQParameter("generated-annotation")
    public void setAnnotation(ACAQTrait annotation) {
        this.annotation = annotation;
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
