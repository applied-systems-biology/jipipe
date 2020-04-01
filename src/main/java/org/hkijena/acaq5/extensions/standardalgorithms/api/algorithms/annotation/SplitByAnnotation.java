package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.annotation;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;

import java.util.List;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@ACAQDocumentation(name = "Split by annotation", description = "Splits the input data by a specified annotation. " +
        "Output slots should correspond to the annotation values. The last slot is reserved for cases where the annotation could not be found.")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Annotation)

// Algorithm traits
public class SplitByAnnotation extends ACAQAlgorithm {

    private ACAQTraitDeclarationRef annotationType = new ACAQTraitDeclarationRef();

    /**
     * @param declaration algorithm declaration
     */
    public SplitByAnnotation(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().restrictInputSlotCount(1).build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public SplitByAnnotation(SplitByAnnotation other) {
        super(other);
        this.annotationType = other.annotationType;
    }

    @Override
    public void run() {
        ACAQDataSlot inputSlot = getFirstInputSlot();
        for (int row = 0; row < inputSlot.getRowCount(); ++row) {
            List<ACAQTrait> annotations = inputSlot.getAnnotations(row);
            ACAQTrait matching = annotations.stream().filter(a -> a.getDeclaration() == annotationType.getDeclaration()).findFirst().orElse(null);

            // Find a matching target slot
            ACAQDataSlot targetSlot;
            if (matching != null) {
                if (matching instanceof ACAQDiscriminator) {
                    targetSlot = getSlots().getOrDefault(((ACAQDiscriminator) matching).getValue(), null);
                    if (targetSlot == null || targetSlot.isInput())
                        targetSlot = getLastOutputSlot();
                } else {
                    targetSlot = getFirstOutputSlot();
                }
            } else {
                targetSlot = getLastOutputSlot();
            }

            targetSlot.addData(inputSlot.getData(row), inputSlot.getAnnotations(row));
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (annotationType.getDeclaration() == null) {
            report.forCategory("Removed annotation").reportIsInvalid("No annotation provided! Please setup an annotation that is removed from the data.");
        } else {
            if (getOutputSlots().size() < 2) {
                if (annotationType.getDeclaration().isDiscriminator())
                    report.forCategory("Output slots").reportIsInvalid("Invalid slot configuration. Please create a slot for data that has the annotation, and a second one for unannotated data.");
                else
                    report.forCategory("Output slots").reportIsInvalid("Invalid slot configuration. Please create a slot for each value to extract, and a second one for non-matching data.");
            }
        }
    }

    @ACAQDocumentation(name = "Annotation", description = "Data is split by this annotation")
    @ACAQParameter("annotation-type")
    public ACAQTraitDeclarationRef getAnnotationType() {
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(ACAQTraitDeclarationRef annotationType) {
        this.annotationType = annotationType;
        getEventBus().post(new ParameterChangedEvent(this, "annotation-type"));
    }
}
