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

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.library.graph.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link ChannelSplitter}
 */
@JIPipeDocumentation(name = "Split channels", description = "Splits multichannel images into multiple greyscale images. " +
        "This operation is applied for each 2D image slice.")
@JIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nColor", aliasName = "Split Channels")
public class NewSplitChannelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final OutputSlotMapParameterCollection channelToSlotAssignment;
    private boolean ignoreMissingChannels = false;

    private OptionalAnnotationNameParameter channelIndexAnnotation = new OptionalAnnotationNameParameter("Channel index", true);

    private OptionalAnnotationNameParameter channelNameAnnotation = new OptionalAnnotationNameParameter("Channel", true);

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
        this.channelIndexAnnotation = new OptionalAnnotationNameParameter(other.channelIndexAnnotation);
        this.channelNameAnnotation = new OptionalAnnotationNameParameter(other.channelNameAnnotation);

        channelToSlotAssignment = new OutputSlotMapParameterCollection(Integer.class, this, (slotInfo) -> 0, false);
        other.channelToSlotAssignment.copyTo(channelToSlotAssignment);
        registerSubParameter(channelToSlotAssignment);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus imp = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
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

            dataBatch.addOutputData(outputSlot, new ImagePlusData(split[channelIndex]), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Channel assignment", description = "Please create an output slot for each greyscale channel you " +
            "want to extract. Then assign the source channel index. The first index is zero.")
    @JIPipeParameter("channel-to-slot-assignments")
    public OutputSlotMapParameterCollection getChannelToSlotAssignment() {
        return channelToSlotAssignment;
    }

    @JIPipeDocumentation(name = "Ignore missing channels", description = "If enabled, the algorithm silently skips invalid assignments like extracting the 4th channel of a 2-channel image. " +
            "If disabled, an error will be thrown if such a condition is detected.")
    @JIPipeParameter("ignore-missing-channels")
    public boolean isIgnoreMissingChannels() {
        return ignoreMissingChannels;
    }

    @JIPipeParameter("ignore-missing-channels")
    public void setIgnoreMissingChannels(boolean ignoreMissingChannels) {
        this.ignoreMissingChannels = ignoreMissingChannels;
    }

    @JIPipeDocumentation(name = "Annotate with channel index", description = "If enabled, create an annotation that contains the channel index (starting with zero)")
    @JIPipeParameter("channel-index-annotation")
    public OptionalAnnotationNameParameter getChannelIndexAnnotation() {
        return channelIndexAnnotation;
    }

    @JIPipeParameter("channel-index-annotation")
    public void setChannelIndexAnnotation(OptionalAnnotationNameParameter channelIndexAnnotation) {
        this.channelIndexAnnotation = channelIndexAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with channel name", description = "If enabled, create an annotation that contains the channel name as defined by the output slot")
    @JIPipeParameter("channel-name-annotation")
    public OptionalAnnotationNameParameter getChannelNameAnnotation() {
        return channelNameAnnotation;
    }

    @JIPipeParameter("channel-name-annotation")
    public void setChannelNameAnnotation(OptionalAnnotationNameParameter channelNameAnnotation) {
        this.channelNameAnnotation = channelNameAnnotation;
    }
}
