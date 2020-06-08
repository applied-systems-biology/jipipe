package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.plugin.ChannelArranger;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.acaq5.extensions.parameters.collections.IntegerIntegerKeyValuePairList;
import org.hkijena.acaq5.extensions.parameters.filters.IntegerIntegerKeyValuePair;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.ChannelArranger}
 */
@ACAQDocumentation(name = "Arrange channels", description = "Reorders the channels of each input image")
@ACAQOrganization(menuPath = "Colors", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusColorData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusColorData.class, slotName = "Output")
public class ArrangeChannelsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private IntegerIntegerKeyValuePairList channelReordering = new IntegerIntegerKeyValuePairList();
    private boolean keepSameChannelCount = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ArrangeChannelsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
                .addInputSlot("Input", ImagePlusColorData.class)
                .addOutputSlot("Output", ImagePlusColorData.class, null)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Creates a copy
     *
     * @param other the other
     */
    public ArrangeChannelsAlgorithm(ArrangeChannelsAlgorithm other) {
        super(other);
        this.channelReordering = new IntegerIntegerKeyValuePairList(other.channelReordering);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus image = dataInterface.getInputData(getFirstInputSlot(), ImagePlusColorData.class).getImage();

        // First determine the number of output channels and generate the order array
        int nChannels;
        if (keepSameChannelCount) {
            nChannels = image.getNChannels();
        } else {
            IntegerIntegerKeyValuePair max = channelReordering.stream().max(Comparator.comparing(IntegerIntegerKeyValuePair::getKey)).orElse(null);
            if (max != null) {
                nChannels = max.getValue() + 1;
            } else {
                nChannels = image.getNChannels();
            }
        }
        int[] order = new int[nChannels]; // Info: Order starts with 1. Map from Array index <- Source channel
        TIntIntMap targetToSourceAssignment = new TIntIntHashMap();
        for (IntegerIntegerKeyValuePair renaming : channelReordering) {
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
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusColorData(result));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (channelReordering.size() > 1) {
            TIntSet generatedTargets = new TIntHashSet();
            IntegerIntegerKeyValuePair max = channelReordering.stream().max(Comparator.comparing(IntegerIntegerKeyValuePair::getKey)).get();

            for (int i = 0; i <= max.getValue(); i++) {
                for (IntegerIntegerKeyValuePair integerIntegerKeyValuePair : channelReordering) {
                    if (integerIntegerKeyValuePair.getValue() == i) {
                        if (generatedTargets.contains(i)) {
                            report.forCategory("Channel reordering").reportIsInvalid("Duplicate reordering targets!",
                                    "The channel " + integerIntegerKeyValuePair.getKey() + " is assigned to channel " + i + ", but it is already assigned.",
                                    "Please check if you have duplicate targets. If you don't have duplicate targets, please note that " +
                                            "channels without instructions are automatically assigned an identity transform. In this case, " +
                                            "you also have to specify where this channel is assigned to.",
                                    this);
                        }
                        generatedTargets.add(i);
                    }
                }
                generatedTargets.add(i);
            }
        }
        for (IntegerIntegerKeyValuePair renaming : channelReordering) {
            if (renaming.getKey() < 0 | renaming.getValue() < 0) {
                report.forCategory("Channel reordering").reportIsInvalid("Invalid channel index!",
                        "A channel index cannot be negative. The first channel index is 0.",
                        "Please update the reordering and remove negative values.",
                        this);
            }
        }


    }

    @ACAQDocumentation(name = "Channel reordering", description = "The channel with index on the left hand side is assigned to the channel with the index on the right hand side. " +
            "The first index is 0. Channels left out of this assignment stay at the same index after transformation.")
    @ACAQParameter("channel-reordering")
    public IntegerIntegerKeyValuePairList getChannelReordering() {
        return channelReordering;
    }

    @ACAQParameter("channel-reordering")
    public void setChannelReordering(IntegerIntegerKeyValuePairList channelReordering) {
        this.channelReordering = channelReordering;
    }

    @ACAQDocumentation(name = "Keep same channel count", description = "If enabled, instructions that change the number of channels are ignored.")
    @ACAQParameter("keep-same-channel-count")
    public boolean isKeepSameChannelCount() {
        return keepSameChannelCount;
    }

    @ACAQParameter("keep-same-channel-count")
    public void setKeepSameChannelCount(boolean keepSameChannelCount) {
        this.keepSameChannelCount = keepSameChannelCount;
    }
}
