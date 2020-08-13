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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color;

import ij.ImagePlus;
import ij.plugin.RGBStackMerge;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.collections.InputSlotMapParameterCollection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;
import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_COLOR_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Merge channels", description = "Merges each greyscale image plane into a multi-channel image. " + "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@JIPipeOrganization(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class MergeChannelsAlgorithm extends JIPipeIteratingAlgorithm {

    private InputSlotMapParameterCollection channelColorAssignment;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MergeChannelsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().restrictInputTo(TO_COLOR_CONVERSION.keySet())
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
     * Instantiates a new node type.
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
            if (channelColorAssignment.getParameters().values().stream().noneMatch(
                    parameterAccess -> parameterAccess.get(ChannelColor.class) == value)) {
                return value;
            }
        }
        return ChannelColor.Red;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus[] channels = new ImagePlus[ChannelColor.values().length];
        for (int i = 0; i < ChannelColor.values().length; ++i) {
            ChannelColor color = ChannelColor.values()[i];
            for (Map.Entry<String, JIPipeParameterAccess> entry : channelColorAssignment.getParameters().entrySet()) {
                ChannelColor entryColor = entry.getValue().get(ChannelColor.class);
                if (entryColor == color) {
                    channels[i] = new ImagePlusGreyscale8UData(dataBatch.getInputData(entry.getKey(), ImagePlusGreyscaleData.class).getImage().duplicate()).getImage();
                }
            }
        }
        ImagePlus merged = RGBStackMerge.mergeChannels(channels, true);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(merged));
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        Set<ChannelColor> existing = new HashSet<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : channelColorAssignment.getParameters().entrySet()) {
            ChannelColor color = entry.getValue().get(ChannelColor.class);
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

    @JIPipeDocumentation(name = "Channel colors", description = "Assigns a color to the specified input slot")
    @JIPipeParameter("channel-color-assignments")
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
