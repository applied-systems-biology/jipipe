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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.color;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.parameters.library.graph.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link ChannelSplitter}
 */
@SetJIPipeDocumentation(name = "Split channels", description = "Splits multichannel images into multiple greyscale images. " +
        "This operation is applied for each 2D image slice.")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nColor", aliasName = "Split Channels")
public class NewSplitChannelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final OutputSlotMapParameterCollection channelToSlotAssignment;
    private boolean ignoreMissingChannels = false;

    private OptionalTextAnnotationNameParameter channelIndexAnnotation = new OptionalTextAnnotationNameParameter("Channel index", true);

    private OptionalTextAnnotationNameParameter channelNameAnnotation = new OptionalTextAnnotationNameParameter("Channel", true);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public NewSplitChannelsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", ImagePlusData.class)
                .restrictOutputTo(JIPipe.getDataTypes().findDataTypesByInterfaces(ImagePlusData.class))
                .sealInput()
                .build());
        channelToSlotAssignment = new OutputSlotMapParameterCollection(Integer.class, this, (slotInfo) -> 0, false);
        channelToSlotAssignment.updateSlots();
        registerSubParameter(channelToSlotAssignment);
    }

    /**
     * Creates a copy
     *
     * @param other the other
     */
    public NewSplitChannelsAlgorithm(NewSplitChannelsAlgorithm other) {
        super(other);
        this.ignoreMissingChannels = other.ignoreMissingChannels;
        this.channelIndexAnnotation = new OptionalTextAnnotationNameParameter(other.channelIndexAnnotation);
        this.channelNameAnnotation = new OptionalTextAnnotationNameParameter(other.channelNameAnnotation);

        channelToSlotAssignment = new OutputSlotMapParameterCollection(Integer.class, this, (slotInfo) -> 0, false);
        other.channelToSlotAssignment.copyTo(channelToSlotAssignment);
        registerSubParameter(channelToSlotAssignment);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imp = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ImagePlus[] split;
        if (!imp.isComposite() && imp.getType() != ImagePlus.COLOR_RGB) {
            imp = new CompositeImage(imp);
        }
        if (imp.isComposite()) {
            split = ChannelSplitter.split(imp);
        } else {
            String title = imp.getTitle();
            Calibration cal = imp.getCalibration();
            ImageStack[] channels = ChannelSplitter.splitRGB(imp.getStack(), true);
            ImagePlus rImp = new ImagePlus(title + " (red)", channels[0]);
            rImp.setCalibration(cal);
            ImagePlus gImp = new ImagePlus(title + " (green)", channels[1]);
            gImp.setCalibration(cal);
            ImagePlus bImp = new ImagePlus(title + " (blue)", channels[2]);
            bImp.setCalibration(cal);
            split = new ImagePlus[]{rImp, gImp, bImp};
        }

        for (JIPipeOutputDataSlot outputSlot : getOutputSlots()) {
            String slotName = outputSlot.getName();
            int channelIndex = channelToSlotAssignment.getParameter(outputSlot.getName(), Integer.class);

            if (channelIndex >= split.length) {
                if (ignoreMissingChannels) {
                    progressInfo.log("Ignoring missing channel index " + channelIndex);
                    continue;
                } else {
                    throw new JIPipeValidationRuntimeException(new IndexOutOfBoundsException("Requested channel " + channelIndex + ", but only " + split.length + " channels are available."),
                            "Could not find channel with index " + channelIndex,
                            "You requested that the input channel " + channelIndex + " should be assigned to slot '" + slotName + "', but there are only " + split.length + " channels available.",
                            "Please check if the index is correct. The first channel index is zero. You can also enable 'Ignore missing channels' to skip such occurrences silently.");
                }
            }

            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            channelIndexAnnotation.addAnnotationIfEnabled(annotations, channelIndex + "");
            channelNameAnnotation.addAnnotationIfEnabled(annotations, slotName);

            iterationStep.addOutputData(outputSlot, new ImagePlusData(split[channelIndex]), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Channel assignment", description = "Please create an output slot for each greyscale channel you " +
            "want to extract. Then assign the source channel index. The first index is zero.")
    @JIPipeParameter("channel-to-slot-assignments")
    public OutputSlotMapParameterCollection getChannelToSlotAssignment() {
        return channelToSlotAssignment;
    }

    @SetJIPipeDocumentation(name = "Ignore missing channels", description = "If enabled, the algorithm silently skips invalid assignments like extracting the 4th channel of a 2-channel image. " +
            "If disabled, an error will be thrown if such a condition is detected.")
    @JIPipeParameter("ignore-missing-channels")
    public boolean isIgnoreMissingChannels() {
        return ignoreMissingChannels;
    }

    @JIPipeParameter("ignore-missing-channels")
    public void setIgnoreMissingChannels(boolean ignoreMissingChannels) {
        this.ignoreMissingChannels = ignoreMissingChannels;
    }

    @SetJIPipeDocumentation(name = "Annotate with channel index", description = "If enabled, create an annotation that contains the channel index (starting with zero)")
    @JIPipeParameter("channel-index-annotation")
    public OptionalTextAnnotationNameParameter getChannelIndexAnnotation() {
        return channelIndexAnnotation;
    }

    @JIPipeParameter("channel-index-annotation")
    public void setChannelIndexAnnotation(OptionalTextAnnotationNameParameter channelIndexAnnotation) {
        this.channelIndexAnnotation = channelIndexAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with channel name", description = "If enabled, create an annotation that contains the channel name as defined by the output slot")
    @JIPipeParameter("channel-name-annotation")
    public OptionalTextAnnotationNameParameter getChannelNameAnnotation() {
        return channelNameAnnotation;
    }

    @JIPipeParameter("channel-name-annotation")
    public void setChannelNameAnnotation(OptionalTextAnnotationNameParameter channelNameAnnotation) {
        this.channelNameAnnotation = channelNameAnnotation;
    }
}
