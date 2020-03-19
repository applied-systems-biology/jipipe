package org.hkijena.acaq5.extension.api.algorithms.annotation;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;

@ACAQDocumentation(name = "Annotate data", description = "Annotates each data with the specified annotation")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Annotation)

// Traits
public class AnnotateAll extends ACAQAlgorithm {

    private ACAQTrait annotation;
    private boolean overwrite = false;

    public AnnotateAll(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQIOSlotConfiguration());
    }

    public AnnotateAll(AnnotateAll other) {
        super(other);
        this.annotation = other.annotation;
        this.overwrite = other.overwrite;
        updateSlotTraits();
    }

    @Override
    public void run() {
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
            report.forCategory("Annotation").reportIsInvalid("No annotation provided! Please setup an annotation that is added to the data.");
        } else if (annotation instanceof ACAQDiscriminator) {
            if (((ACAQDiscriminator) annotation).getValue() == null)
                report.forCategory("Annotation").reportIsInvalid("No annotation value provided! Please add a meaningful value that separates your data from other data.");
        }
    }

    private void updateSlotTraits() {
        ACAQDefaultMutableTraitConfiguration traitConfiguration = (ACAQDefaultMutableTraitConfiguration) getTraitConfiguration();
        traitConfiguration.getMutableGlobalTraitModificationTasks().clear();
        if (annotation != null)
            traitConfiguration.getMutableGlobalTraitModificationTasks().set(annotation.getDeclaration(), ACAQTraitModificationOperation.Add);
        traitConfiguration.postChangedEvent();
    }

    @ACAQDocumentation(name = "Annotation", description = "This annotation is added to each input data")
    @ACAQParameter("generated-annotation")
    public ACAQTrait getAnnotation() {
        return annotation;
    }

    @ACAQParameter("generated-annotation")
    public void setAnnotation(ACAQTrait annotation) {
        this.annotation = annotation;
        updateSlotTraits();
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
