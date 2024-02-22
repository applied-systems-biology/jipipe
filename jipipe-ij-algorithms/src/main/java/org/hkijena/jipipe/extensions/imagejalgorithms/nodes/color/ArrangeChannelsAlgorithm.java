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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.color;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.plugin.ChannelArranger;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.IntegerAndIntegerPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;

/**
 * Wrapper around {@link ij.plugin.ChannelArranger}
 */
@SetJIPipeDocumentation(name = "Arrange channels", description = "Reorders the channels of each input image")
@DefineJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nColor", aliasName = "Arrange Channels...")
public class ArrangeChannelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntegerAndIntegerPairParameter.List channelReordering = new IntegerAndIntegerPairParameter.List();
    private boolean keepSameChannelCount = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ArrangeChannelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the other
     */
    public ArrangeChannelsAlgorithm(ArrangeChannelsAlgorithm other) {
        super(other);
        this.channelReordering = new IntegerAndIntegerPairParameter.List(other.channelReordering);
        this.keepSameChannelCount = other.keepSameChannelCount;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus image = inputData.getImage();

        if (image.getNDimensions() == 3) {
            // Ensure that the image is channel
            image = inputData.getDuplicateImage();
            image.setDimensions(image.getStackSize(), 1, 1);
        }

        // First determine the number of output channels and generate the order array
        int nChannels;
        if (keepSameChannelCount) {
            nChannels = image.getNChannels();
        } else {
            IntegerAndIntegerPairParameter max = channelReordering.stream().max(Comparator.comparing(IntegerAndIntegerPairParameter::getKey)).orElse(null);
            if (max != null) {
                nChannels = max.getValue() + 1;
            } else {
                nChannels = image.getNChannels();
            }
        }
        int[] order = new int[nChannels]; // Info: Order starts with 1. Map from Array index <- Source channel
        TIntIntMap targetToSourceAssignment = new TIntIntHashMap();
        for (IntegerAndIntegerPairParameter renaming : channelReordering) {
            targetToSourceAssignment.put(renaming.getValue(), renaming.getKey());
        }

        // Create the order vector
        for (int targetChannelIndex = 0; targetChannelIndex < nChannels; targetChannelIndex++) {
            int source;
            if (targetToSourceAssignment.containsKey(targetChannelIndex)) {
                source = targetToSourceAssignment.get(targetChannelIndex);
            } else {
                source = targetChannelIndex;
            }

            // Clamp to be within range and add 1 (for the plugin)
            source = Math.min(source, image.getNChannels() - 1) + 1;
            order[targetChannelIndex] = source;
        }

        ImagePlus result = ChannelArranger.run(image, order);
        ImageJUtils.calibrate(result, ImageJCalibrationMode.AutomaticImageJ, 0, 0);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }


    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (channelReordering.size() > 1) {
            TIntSet generatedTargets = new TIntHashSet();
            IntegerAndIntegerPairParameter max = channelReordering.stream().max(Comparator.comparing(IntegerAndIntegerPairParameter::getKey)).get();

            for (int i = 0; i <= max.getValue(); i++) {
                for (IntegerAndIntegerPairParameter integerAndIntegerPair : channelReordering) {
                    if (integerAndIntegerPair.getValue() == i) {
                        if (generatedTargets.contains(i)) {
                            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                    new ParameterValidationReportContext(this, "Channel reordering", "channel-reordering"),
                                    "The channel " + integerAndIntegerPair.getKey() + " is assigned to channel " + i + ", but it is already assigned.",
                                    "Please check if you have duplicate targets. If you don't have duplicate targets, please note that " +
                                            "channels without instructions are automatically assigned an identity transform. In this case, " +
                                            "you also have to specify where this channel is assigned to."));
                        }
                        generatedTargets.add(i);
                    }
                }
                generatedTargets.add(i);
            }
        }
        for (IntegerAndIntegerPairParameter renaming : channelReordering) {
            if (renaming.getKey() < 0 | renaming.getValue() < 0) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new ParameterValidationReportContext(this, "Channel reordering", "channel-reordering"),
                        "Invalid channel index!",
                        "A channel index cannot be negative. The first channel index is 0.",
                        "Please update the reordering and remove negative values."));
            }
        }


    }

    @SetJIPipeDocumentation(name = "Channel reordering", description = "The channel with index on the left hand side is assigned to the channel with the index on the right hand side. " +
            "The first index is 0. Channels left out of this assignment stay at the same index after transformation.")
    @JIPipeParameter("channel-reordering")
    @PairParameterSettings(singleRow = false, keyLabel = "Source channel", valueLabel = "Target channel")
    public IntegerAndIntegerPairParameter.List getChannelReordering() {
        return channelReordering;
    }

    @JIPipeParameter("channel-reordering")
    public void setChannelReordering(IntegerAndIntegerPairParameter.List channelReordering) {
        this.channelReordering = channelReordering;
    }

    @SetJIPipeDocumentation(name = "Keep same channel count", description = "If enabled, instructions that change the number of channels are ignored.")
    @JIPipeParameter("keep-same-channel-count")
    public boolean isKeepSameChannelCount() {
        return keepSameChannelCount;
    }

    @JIPipeParameter("keep-same-channel-count")
    public void setKeepSameChannelCount(boolean keepSameChannelCount) {
        this.keepSameChannelCount = keepSameChannelCount;
    }

    @SetJIPipeDocumentation(name = "Simple reorder", description = "Allows you to input the reordering like in ImageJ.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/imagej.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/imagej.png")
    public void setToExample(JIPipeWorkbench parent) {
        String reordering = JOptionPane.showInputDialog(parent.getWindow(),
                "Please put in the list of numbers how the channels should be re-ordered. " +
                        "For example '1,2,3'.\n\nPlease note that the first channel index is one (like in ImageJ).\nThe 'Channel reordering' " +
                        "parameter indexes channels starting from zero.",
                "1,2,3");
        if (reordering == null)
            return;
        IntegerRange range = new IntegerRange(reordering);
        List<Integer> channelIndices = range.tryGetIntegers(0, 0, new JIPipeExpressionVariablesMap());
        if (channelIndices == null || channelIndices.isEmpty() || channelIndices.stream().anyMatch(i -> i <= 0)) {
            JOptionPane.showMessageDialog(parent.getWindow(), "Invalid channel indices. Please provide a comma separated list of positive numbers.");
            return;
        }
        channelReordering.clear();
        for (int i = 0; i < channelIndices.size(); i++) {
            IntegerAndIntegerPairParameter pair = new IntegerAndIntegerPairParameter();
            pair.setValue(i);
            pair.setKey(channelIndices.get(i) - 1);
            channelReordering.add(pair);
        }
        emitParameterChangedEvent("channel-reordering");
    }
}
