/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelArranger;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.library.graph.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ChannelArranger}
 */
@SetJIPipeDocumentation(name = "Reduce & split stacks (slice)", description = "Splits incoming stacks into a customizable amount of stacks based on stack indices. Add more output slots " +
        "to create more groups. Please note that this node utilizes slice indices and cannot handle 5D images. Please use 'Reduce & split hyperstack' or 'Reduce & split hyperstack (Expression)' if you want to properly handle 5D images.")
@ConfigureJIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks")
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
                .addInputSlot("Input", "", ImagePlusData.class)
                .addOutputSlot("Output", "", ImagePlusData.class, "Input")
                .sealInput()
                .build());
        stackAssignments = new OutputSlotMapParameterCollection(IntegerRange.class,
                this);
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
                this);
        other.stackAssignments.copyTo(stackAssignments);
        registerSubParameter(stackAssignments);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (Map.Entry<String, JIPipeParameterAccess> entry : stackAssignments.getParameters().entrySet()) {
            IntegerRange sliceSelection = entry.getValue().get(IntegerRange.class);
            List<Integer> sliceIndices = sliceSelection.getIntegers(0, img.getStackSize(), variables);
            if (ignoreMissingSlices) {
                sliceIndices.removeIf(i -> i >= img.getStackSize());
            } else {
                for (Integer integer : sliceIndices) {
                    if (integer >= img.getStackSize()) {
                        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                                "Data does not have slice: " + integer,
                                "The algorithm was set up to select slice " + integer + ", but this slice does not exist. The image only has " + img.getStackSize() + " slices.",
                                "Please check if the incoming data has at least the amount of slices as requested. Please do not forget that the first slice index is zero. " +
                                        "If you are sure what you do, enable 'Ignore missing slices' in the algorithm settings."));
                    }
                }
            }
            if (sliceIndices.isEmpty()) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                        "No slices selected!",
                        "You have to select a valid set of slices from the data set.",
                        "Please check if the incoming data has at least the amount of slices as requested."));
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
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (!StringUtils.isNullOrEmpty(annotationType)) {
                String index = "slice=" + sliceIndices.stream().map(i -> "" + i).collect(Collectors.joining(","));
                annotations.add(new JIPipeTextAnnotation(annotationType, index));
            }
            result.copyScale(img);
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Ignore missing slices", description = "If enabled, slice indices outside of the image dimensions are ignored.")
    @JIPipeParameter("ignore-missing-slices")
    public boolean isIgnoreMissingSlices() {
        return ignoreMissingSlices;
    }

    @JIPipeParameter("ignore-missing-slices")
    public void setIgnoreMissingSlices(boolean ignoreMissingSlices) {
        this.ignoreMissingSlices = ignoreMissingSlices;
    }

    @SetJIPipeDocumentation(name = "Stack assignment", description = "Each output slot is assigned to a set of stacks that is determined by the " +
            "selection on the right-hand side. You have to input a valid number range (e.g. 0-10;15;22-23) " +
            "that then will then generated as output of the corresponding data slot. Inverse ordered ranges (e.g. 10-5) are supported. " +
            "The first slice is indexed with 0.")
    @JIPipeParameter("stack-assignments")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public OutputSlotMapParameterCollection getStackAssignments() {
        return stackAssignments;
    }

    @SetJIPipeDocumentation(name = "Generated annotation", description = "An optional annotation that is generated for each output to indicate from which slices the data was generated from. " +
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

    @SetJIPipeDocumentation(name = "Sort stack indices", description = "If enabled, stack indices are sorted before they are used to create an output.")
    @JIPipeParameter("sorted-stack-ids")
    public boolean isSortedStackIds() {
        return sortedStackIds;
    }

    @JIPipeParameter("sorted-stack-ids")
    public void setSortedStackIds(boolean sortedStackIds) {
        this.sortedStackIds = sortedStackIds;
    }

    @SetJIPipeDocumentation(name = "Unique stack indices", description = "If enabled, duplicate stack indices are removed before they are used to create an output.")
    @JIPipeParameter("sorted-stack-ids")
    public boolean isUniqueStackIds() {
        return uniqueStackIds;
    }

    public void setUniqueStackIds(boolean uniqueStackIds) {
        this.uniqueStackIds = uniqueStackIds;
    }
}
