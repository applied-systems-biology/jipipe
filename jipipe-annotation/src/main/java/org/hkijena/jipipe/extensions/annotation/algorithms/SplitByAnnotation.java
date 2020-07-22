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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.categories.AnnotationNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@JIPipeDocumentation(name = "Split & filter by annotation", description = "Splits the input data by a specified annotation or filters data based on the annotation value.")
@JIPipeOrganization(nodeTypeCategory = AnnotationNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input")
public class SplitByAnnotation extends JIPipeAlgorithm {

    private String annotationType = "";
    private OutputSlotMapParameterCollection targetSlots;
    private boolean enableFallthrough = false;

    /**
     * @param info algorithm info
     */
    public SplitByAnnotation(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", JIPipeData.class)
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
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        JIPipeDataSlot inputSlot = getFirstInputSlot();
        for (int row = 0; row < inputSlot.getRowCount(); ++row) {
            List<JIPipeAnnotation> annotations = inputSlot.getAnnotations(row);
            JIPipeAnnotation matching = annotations.stream().filter(a -> a.nameEquals(annotationType)).findFirst().orElse(null);
            if (matching != null) {
                String matchingValue = matching.getValue();

                for (JIPipeDataSlot slot : getOutputSlots().stream().sorted(Comparator.comparing(JIPipeDataSlot::getName)).collect(Collectors.toList())) {
                    StringPredicate filter = targetSlots.getParameters().get(slot.getName()).get(StringPredicate.class);
                    if (filter.test(matchingValue)) {
                        slot.addData(inputSlot.getData(row, JIPipeData.class), inputSlot.getAnnotations(row));
                        if (!enableFallthrough)
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
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

    @JIPipeDocumentation(name = "Annotation", description = "Data is split by this annotation")
    @JIPipeParameter("annotation-type")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getAnnotationType() {
        return annotationType;
    }

    @JIPipeParameter("annotation-type")
    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;

    }

    @JIPipeParameter("target-slots")
    @JIPipeDocumentation(name = "Target slots", description = "Annotation values that match the filter on the right-hand side are redirected to the data slot on the left-hand side. " +
            "Use the the RegEx filter '.*' to filter remaining inputs. Filter order is alphabetically.")
    public OutputSlotMapParameterCollection getTargetSlots() {
        return targetSlots;
    }

    @JIPipeDocumentation(name = "Continue after filter matches", description = "Continue with other filters if a matching filter was found")
    @JIPipeParameter("enable-fallthrough")
    public boolean isEnableFallthrough() {
        return enableFallthrough;
    }

    @JIPipeParameter("enable-fallthrough")
    public void setEnableFallthrough(boolean enableFallthrough) {
        this.enableFallthrough = enableFallthrough;

    }
}
