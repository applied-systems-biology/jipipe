package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color;

import ij.ImagePlus;
import ij.plugin.RGBStackMerge;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.acaq5.extensions.parameters.collections.InputSlotMapParameterCollection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_COLOR_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Merge channels", description = "Merges each greyscale image plane into a multi-channel image")
@ACAQOrganization(menuPath = "Colors", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class MergeChannelsAlgorithm extends ImageJ1Algorithm {

    private InputSlotMapParameterCollection channelColorAssignment;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public MergeChannelsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().restrictInputTo(TO_COLOR_CONVERSION.keySet())
                .restrictInputSlotCount(ChannelColor.values().length)
                .addOutputSlot("Output", ImagePlusColorRGBData.class, "Input", TO_COLOR_CONVERSION)
                .allowOutputSlotInheritance(true)
                .sealOutput()
                .build());
        channelColorAssignment = new InputSlotMapParameterCollection(ChannelColor.class, this, this::getNewChannelColor, false);
        channelColorAssignment.updateSlots();
        registerSubParameter(channelColorAssignment);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public MergeChannelsAlgorithm(MergeChannelsAlgorithm other) {
        super(other);
        channelColorAssignment = new InputSlotMapParameterCollection(ChannelColor.class, this, this::getNewChannelColor, false);
        other.channelColorAssignment.copyTo(channelColorAssignment);
        registerSubParameter(channelColorAssignment);
    }

    private ChannelColor getNewChannelColor() {
        for (ChannelColor value : ChannelColor.values()) {
            if (channelColorAssignment.getParameters().values().stream().noneMatch(parameterAccess -> parameterAccess.get() == value)) {
                return value;
            }
        }
        return ChannelColor.Red;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus[] channels = new ImagePlus[ChannelColor.values().length];
        for (int i = 0; i < ChannelColor.values().length; ++i) {
            ChannelColor color = ChannelColor.values()[i];
            for (Map.Entry<String, ACAQParameterAccess> entry : channelColorAssignment.getParameters().entrySet()) {
                ChannelColor entryColor = entry.getValue().get();
                if (entryColor == color) {
                    channels[i] = dataInterface.getInputData(entry.getKey(), ImagePlusGreyscaleData.class).getImage();
                }
            }
        }
        ImagePlus merged = RGBStackMerge.mergeChannels(channels, true);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(merged));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        Set<ChannelColor> existing = new HashSet<>();
        for (Map.Entry<String, ACAQParameterAccess> entry : channelColorAssignment.getParameters().entrySet()) {
            ChannelColor color = entry.getValue().get();
            report.forCategory("Channel colors").forCategory(entry.getKey()).checkNonNull(color, this);
            if (color != null) {
                if (existing.contains(color))
                    report.forCategory("Channel colors").forCategory(entry.getKey()).reportIsInvalid("Duplicate color assignment!",
                            "Color '" + color + "' is already assigned.",
                            "Please assign another color.",
                            this);
                existing.add(color);
            }
        }
    }

    @ACAQDocumentation(name = "Channel colors", description = "Assigns a color to the specified input slot")
    @ACAQParameter("channel-color-assignments")
    public InputSlotMapParameterCollection getChannelColorAssignment() {
        return channelColorAssignment;
    }

    /**
     * Color a slice can be mapped to
     */
    public enum ChannelColor {
        Red,
        Green,
        Blue,
        Gray,
        Cyan,
        Magenta,
        Yellow
    }
}
