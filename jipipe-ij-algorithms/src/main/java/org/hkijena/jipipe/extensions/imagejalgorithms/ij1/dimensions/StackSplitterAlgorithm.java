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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelArranger;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ChannelArranger}
 */
@JIPipeDocumentation(name = "Reduce & split stacks", description = "Splits incoming stacks into a customizable amount of stacks based on stack indices. Add more output slots " +
        "to create more groups.")
@JIPipeOrganization(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
public class StackSplitterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OutputSlotMapParameterCollection stackAssignments;
    private boolean ignoreMissingSlices = false;
    private boolean sortedStackIds = true;
    private boolean uniqueStackIds = true;
    private String annotationType = "Image index";

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StackSplitterAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input")
                .sealInput()
                .build());
        stackAssignments = new OutputSlotMapParameterCollection(IntegerRange.class,
                this,
                IntegerRange::new,
                false);
        stackAssignments.updateSlots();
        registerSubParameter(stackAssignments);
    }

    /**
     * Creates a copy
     *
     * @param other the other
     */
    public StackSplitterAlgorithm(StackSplitterAlgorithm other) {
        super(other);
        this.ignoreMissingSlices = other.ignoreMissingSlices;
        this.sortedStackIds = other.sortedStackIds;
        this.uniqueStackIds = other.uniqueStackIds;
        this.annotationType = other.annotationType;

        stackAssignments = new OutputSlotMapParameterCollection(IntegerRange.class,
                this,
                IntegerRange::new,
                false);
        other.stackAssignments.copyTo(stackAssignments);
        registerSubParameter(stackAssignments);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        for (Map.Entry<String, JIPipeParameterAccess> entry : stackAssignments.getParameters().entrySet()) {
            IntegerRange sliceSelection = entry.getValue().get(IntegerRange.class);
            List<Integer> sliceIndices = sliceSelection.getIntegers();
            if (ignoreMissingSlices) {
                sliceIndices.removeIf(i -> i >= img.getStackSize());
            } else {
                for (Integer integer : sliceIndices) {
                    if (integer >= img.getStackSize()) {
                        throw new UserFriendlyRuntimeException("Data does not have slice: " + integer,
                                "Invalid slice requested!",
                                "Algorithm '" + getName() + "'",
                                "The algorithm was set up to select slice " + integer + ", but this slice does not exist. The image only has " + img.getStackSize() + " slices.",
                                "Please check if the incoming data has at least the amount of slices as requested. Please do not forget that the first slice index is zero. " +
                                        "If you are sure what you do, enable 'Ignore missing slices' in the algorithm settings.");
                    }
                }
            }
            if (sliceIndices.isEmpty()) {
                throw new UserFriendlyRuntimeException("No slices selected!",
                        "No slices selected!",
                        "Algorithm '" + getName() + "'",
                        "You have to select a valid set of slices from the data set.",
                        "Please check if the incoming data has at least the amount of slices as requested.");
            }
            if (uniqueStackIds) {
                sliceIndices = sliceIndices.stream().distinct().collect(Collectors.toList());
            }
            if (sortedStackIds) {
                sliceIndices.sort(Integer::compareTo);
            }

            ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
            for (Integer sliceIndex : sliceIndices) {
                stack.addSlice("" + sliceIndex, img.getStack().getProcessor(sliceIndex + 1).duplicate());
            }
            ImagePlus result = new ImagePlus("Reduced stack", stack);
            List<JIPipeAnnotation> annotations = new ArrayList<>();
            if (!StringUtils.isNullOrEmpty(annotationType)) {
                String index = "slice=" + sliceIndices.stream().map(i -> "" + i).collect(Collectors.joining(","));
                annotations.add(new JIPipeAnnotation(annotationType, index));
            }
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        for (Map.Entry<String, JIPipeParameterAccess> entry : stackAssignments.getParameters().entrySet()) {
            IntegerRange sliceSelection = entry.getValue().get(IntegerRange.class);
            try {
                List<Integer> integers = sliceSelection.getIntegers();
                if (integers.isEmpty()) {
                    report.resolve("Stack assignment").resolve(entry.getKey()).reportIsInvalid("No slices selected!",
                            "You have to select at least one slice.",
                            "Please enter a valid selection (e.g. 10-15)",
                            this);
                }
                for (Integer integer : integers) {
                    if (integer < 0) {
                        report.resolve("Stack assignment").resolve(entry.getKey()).reportIsInvalid("Slice indices cannot be negative!",
                                "The first slice index is 0. Negative indices are not valid.",
                                "Please enter a valid selection (e.g. 10-15)",
                                this);
                        break;
                    }
                }

            } catch (NumberFormatException | NullPointerException e) {
                report.resolve("Stack assignment").resolve(entry.getKey()).reportIsInvalid("Wrong slice index format!",
                        "The slice indices must follow a specific pattern. " + "The format is: [range];[range];... where [range] is " +
                                "either a number or a range of numbers notated as [from]-[to] (inclusive). Inverse ordered ranges are allowed." +
                                " An example is 0-10;12;20-21. The first index is zero.",
                        "Please enter a valid selection (e.g. 10-15)",
                        this);
            }
        }
    }

    @JIPipeDocumentation(name = "Ignore missing slices", description = "If enabled, slice indices outside of the image dimensions are ignored.")
    @JIPipeParameter("ignore-missing-slices")
    public boolean isIgnoreMissingSlices() {
        return ignoreMissingSlices;
    }

    @JIPipeParameter("ignore-missing-slices")
    public void setIgnoreMissingSlices(boolean ignoreMissingSlices) {
        this.ignoreMissingSlices = ignoreMissingSlices;
    }

    @JIPipeDocumentation(name = "Stack assignment", description = "Each output slot is assigned to a set of stacks that is determined by the " +
            "selection on the right-hand side. You have to input a valid number range (e.g. 0-10;15;22-23) " +
            "that then will then generated as output of the corresponding data slot. Inverse ordered ranges (e.g. 10-5) are supported. " +
            "The first slice is indexed with 0.")
    @JIPipeParameter("stack-assignments")
    public OutputSlotMapParameterCollection getStackAssignments() {
        return stackAssignments;
    }

    @JIPipeDocumentation(name = "Generated annotation", description = "An optional annotation that is generated for each output to indicate from which slices the data was generated from. " +
            "The format will be slice=[index0],[index1],...")
    @JIPipeParameter("annotation-type")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getAnnotationType() {
        return annotationType;
    }

    @JIPipeParameter("annotation-type")
    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }

    @JIPipeDocumentation(name = "Sort stack indices", description = "If enabled, stack indices are sorted before they are used to create an output.")
    @JIPipeParameter("sorted-stack-ids")
    public boolean isSortedStackIds() {
        return sortedStackIds;
    }

    @JIPipeParameter("sorted-stack-ids")
    public void setSortedStackIds(boolean sortedStackIds) {
        this.sortedStackIds = sortedStackIds;
    }

    @JIPipeDocumentation(name = "Unique stack indices", description = "If enabled, duplicate stack indices are removed before they are used to create an output.")
    @JIPipeParameter("sorted-stack-ids")
    public boolean isUniqueStackIds() {
        return uniqueStackIds;
    }

    public void setUniqueStackIds(boolean uniqueStackIds) {
        this.uniqueStackIds = uniqueStackIds;
    }
}
