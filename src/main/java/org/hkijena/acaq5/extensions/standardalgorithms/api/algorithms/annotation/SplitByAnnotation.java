package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.annotation;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@ACAQDocumentation(name = "Split & filter by annotation", description = "Splits the input data by a specified annotation or filters data based on the annotation value.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation)

// Algorithm traits
public class SplitByAnnotation extends ACAQAlgorithm {

    private ACAQTraitDeclarationRef annotationType = new ACAQTraitDeclarationRef();
    private OutputSlotMapParameterCollection targetSlots;
    private boolean enableFallthrough = false;

    /**
     * @param declaration algorithm declaration
     */
    public SplitByAnnotation(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().restrictInputSlotCount(1).build());
        this.targetSlots = new OutputSlotMapParameterCollection(StringFilter.class, this, null, true);
        this.targetSlots.getEventBus().register(this);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public SplitByAnnotation(SplitByAnnotation other) {
        super(other);
        this.enableFallthrough = other.enableFallthrough;
        this.annotationType = other.annotationType;
        this.targetSlots = new OutputSlotMapParameterCollection(String.class, this, null, false);
        other.targetSlots.copyTo(this.targetSlots);
        this.targetSlots.getEventBus().register(this);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ACAQDataSlot inputSlot = getFirstInputSlot();
        for (int row = 0; row < inputSlot.getRowCount(); ++row) {
            List<ACAQTrait> annotations = inputSlot.getAnnotations(row);
            ACAQTrait matching = annotations.stream().filter(a -> a.getDeclaration() == annotationType.getDeclaration()).findFirst().orElse(null);
            String matchingValue;
            if (annotationType.getDeclaration().isDiscriminator()) {
                matchingValue = matching instanceof ACAQDiscriminator ? "" + ((ACAQDiscriminator) matching).getValue() : "";
            } else {
                matchingValue = matching != null ? "true" : "false";
            }

            for (ACAQDataSlot slot : getOutputSlots().stream().sorted(Comparator.comparing(ACAQDataSlot::getName)).collect(Collectors.toList())) {
                StringFilter filter = targetSlots.getParameters().get(slot.getName()).get(StringFilter.class);
                if (filter.test(matchingValue)) {
                    slot.addData(inputSlot.getData(row, ACAQData.class), inputSlot.getAnnotations(row));
                    if (!enableFallthrough)
                        break;
                }
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (annotationType == null || annotationType.getDeclaration() == null) {
            report.forCategory("Annotation").reportIsInvalid("No annotation provided!",
                    "You have to determine by which annotation type the data should be split.",
                    "Please provide an annotation type.",
                    this);
        } else {
            if (getOutputSlots().isEmpty()) {
                report.forCategory("Output").reportIsInvalid("No output slots defined!",
                        "The split/filtered data is put into the output slot(s).",
                        "Please add at least one output slot.",
                        this);
            }
        }
    }

    @ACAQDocumentation(name = "Annotation", description = "Data is split by this annotation")
    @ACAQParameter("annotation-type")
    public ACAQTraitDeclarationRef getAnnotationType() {
        if (annotationType == null)
            annotationType = new ACAQTraitDeclarationRef();
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(ACAQTraitDeclarationRef annotationType) {
        this.annotationType = annotationType;
        getEventBus().post(new ParameterChangedEvent(this, "annotation-type"));
    }

    @ACAQParameter("target-slots")
    @ACAQDocumentation(name = "Target slots", description = "Annotation values that match the filter on the right-hand side are redirected to the data slot on the left-hand side. " +
            "Non-value annotations are converted into 'true' and 'false'. Use the the RegEx filter '.*' to filter remaining inputs. Filter order is alphabetically.")
    public OutputSlotMapParameterCollection getTargetSlots() {
        return targetSlots;
    }

    @ACAQDocumentation(name = "Continue after filter matches", description = "Continue with other filters if a matching filter was found")
    @ACAQParameter("enable-fallthrough")
    public boolean isEnableFallthrough() {
        return enableFallthrough;
    }

    @ACAQParameter("enable-fallthrough")
    public void setEnableFallthrough(boolean enableFallthrough) {
        this.enableFallthrough = enableFallthrough;
        getEventBus().post(new ParameterChangedEvent(this, "enable-fallthrough"));
    }
}
