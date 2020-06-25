/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;

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

    private String annotationType = "";
    private OutputSlotMapParameterCollection targetSlots;
    private boolean enableFallthrough = false;

    /**
     * @param declaration algorithm declaration
     */
    public SplitByAnnotation(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", ACAQData.class)
                .sealInput()
                .build());
        this.targetSlots = new OutputSlotMapParameterCollection(StringPredicate.class, this, null, true);
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
            List<ACAQAnnotation> annotations = inputSlot.getAnnotations(row);
            ACAQAnnotation matching = annotations.stream().filter(a -> a.nameEquals(annotationType)).findFirst().orElse(null);
            if (matching != null) {
                String matchingValue = matching.getValue();

                for (ACAQDataSlot slot : getOutputSlots().stream().sorted(Comparator.comparing(ACAQDataSlot::getName)).collect(Collectors.toList())) {
                    StringPredicate filter = targetSlots.getParameters().get(slot.getName()).get(StringPredicate.class);
                    if (filter.test(matchingValue)) {
                        slot.addData(inputSlot.getData(row, ACAQData.class), inputSlot.getAnnotations(row));
                        if (!enableFallthrough)
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (StringUtils.isNullOrEmpty(annotationType)) {
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
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getAnnotationType() {
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
        getEventBus().post(new ParameterChangedEvent(this, "annotation-type"));
    }

    @ACAQParameter("target-slots")
    @ACAQDocumentation(name = "Target slots", description = "Annotation values that match the filter on the right-hand side are redirected to the data slot on the left-hand side. " +
            "Use the the RegEx filter '.*' to filter remaining inputs. Filter order is alphabetically.")
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
