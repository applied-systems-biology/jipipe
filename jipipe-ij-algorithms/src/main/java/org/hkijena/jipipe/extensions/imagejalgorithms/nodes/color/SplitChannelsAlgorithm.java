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

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelSplitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.graph.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Wrapper around {@link ChannelSplitter}
 */
@JIPipeDocumentation(name = "Split channels (deprecated)", description = "Splits multichannel images into multiple greyscale images. " +
        "This operation is applied for each 2D image slice. <strong>This node is deprecated. Please use the new 'Split channels' node</strong>")
@JIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nColor", aliasName = "Split Channels")
@JIPipeHidden
@Deprecated
public class SplitChannelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final OutputSlotMapParameterCollection channelToSlotAssignment;
    private boolean ignoreMissingChannels = false;
    private String annotationColumnSlotName = "Channel";
    private String annotationColumnChannelIndex = "Channel index";
    private boolean annotateWithChannelIndex = true;
    private boolean annotateWithSlotName = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public SplitChannelsAlgorithm(JIPipeNodeInfo info) {
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
    public SplitChannelsAlgorithm(SplitChannelsAlgorithm other) {
        super(other);
        this.annotationColumnSlotName = other.annotationColumnSlotName;
        this.annotationColumnChannelIndex = other.annotationColumnChannelIndex;
        this.ignoreMissingChannels = other.ignoreMissingChannels;
        this.annotateWithSlotName = other.annotateWithSlotName;
        this.annotateWithChannelIndex = other.annotateWithChannelIndex;

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
        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();

        // If we have a grayscale image then we can just skip everything
        if (image.getNChannels() == 1 && image.getType() != ImagePlus.COLOR_256 && image.getType() != ImagePlus.COLOR_RGB) {
            int nChannels = 1;
            for (Map.Entry<String, JIPipeParameterAccess> entry : channelToSlotAssignment.getParameters().entrySet()) {
                String slotName = entry.getKey();
                int channelIndex = entry.getValue().get(Integer.class);

                if (channelIndex >= nChannels) {
                    if (ignoreMissingChannels) {
                        continue;
                    } else {
                        throw new UserFriendlyRuntimeException(new IndexOutOfBoundsException("Requested channel " + channelIndex + ", but only " + nChannels + " channels are available."),
                                "Could not find channel with index " + channelIndex,
                                "'Split channels' algorithm, slot '" + slotName + "'",
                                "You requested that the input channel " + channelIndex + " should be assigned to slot '" + slotName + "', but there are only " + nChannels + " channels available.",
                                "Please check if the index is correct. The first channel index is zero. You can also enable 'Ignore missing channels' to skip such occurrences silently.");
                    }
                }

                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (annotateWithSlotName) {
                    annotations.add(new JIPipeTextAnnotation(annotationColumnSlotName, slotName));
                }
                if (annotateWithChannelIndex) {
                    annotations.add(new JIPipeTextAnnotation(annotationColumnChannelIndex, "" + channelIndex));
                }
                dataBatch.addOutputData(slotName, new ImagePlusGreyscaleData(image), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
            return;
        }

        // First, we need to ensure that we only have 2D grayscale planes
        // This means we have to completely decompose the image
        Map<ImageSliceIndex, ImageProcessor> decomposedSlices = new HashMap<>();
        ImageJUtils.forEachIndexedZTSlice(image, (channels, sliceIndex) -> {
            // Decompose potential nested color processors
            // We might need to add some channels as we de-compose color processors
            int correctedChannel = 0;
            Map<Integer, ImageProcessor> decomposed = new HashMap<>();
            for (Map.Entry<Integer, ImageProcessor> entry : channels.entrySet()) {
                ImageProcessor processor = entry.getValue();
                if (processor instanceof ColorProcessor) {
                    ImagePlus nestedMultiChannel = new ImagePlus("nested", processor);
                    ImagePlus[] split = ChannelSplitter.split(nestedMultiChannel);
                    for (ImagePlus imagePlus : split) {
                        decomposed.put(entry.getKey() + correctedChannel, imagePlus.getProcessor());
                        ++correctedChannel;
                    }
                } else {
                    decomposed.put(entry.getKey() + correctedChannel, processor);
                }
            }
            for (Map.Entry<Integer, ImageProcessor> entry : decomposed.entrySet()) {
                decomposedSlices.put(new ImageSliceIndex(entry.getKey(), sliceIndex.getZ(), sliceIndex.getT()), entry.getValue());
            }
        }, progressInfo);
        int nChannels = decomposedSlices.keySet().stream().map(ImageSliceIndex::getC).max(Comparator.naturalOrder()).orElse(-1) + 1;

        for (Map.Entry<String, JIPipeParameterAccess> entry : channelToSlotAssignment.getParameters().entrySet()) {
            String slotName = entry.getKey();
            int channelIndex = entry.getValue().get(Integer.class);

            if (channelIndex >= nChannels) {
                if (ignoreMissingChannels) {
                    continue;
                } else {
                    throw new UserFriendlyRuntimeException(new IndexOutOfBoundsException("Requested channel " + channelIndex + ", but only " + nChannels + " channels are available."),
                            "Could not find channel with index " + channelIndex,
                            "'Split channels' algorithm, slot '" + slotName + "'",
                            "You requested that the input channel " + channelIndex + " should be assigned to slot '" + slotName + "', but there are only " + nChannels + " channels available.",
                            "Please check if the index is correct. The first channel index is zero. You can also enable 'Ignore missing channels' to skip such occurrences silently.");
                }
            }

            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (annotateWithSlotName) {
                annotations.add(new JIPipeTextAnnotation(annotationColumnSlotName, slotName));
            }
            if (annotateWithChannelIndex) {
                annotations.add(new JIPipeTextAnnotation(annotationColumnChannelIndex, "" + channelIndex));
            }

            // Rebuild image stack
            ImageStack stack = new ImageStack(image.getWidth(), image.getHeight(), image.getNSlices() * image.getNFrames());
            ImageSliceIndex tempIndex = new ImageSliceIndex();
            tempIndex.setC(channelIndex);
            for (int t = 0; t < image.getNFrames(); t++) {
                tempIndex.setT(t);
                for (int z = 0; z < image.getNSlices(); z++) {
                    tempIndex.setZ(z);
                    ImageProcessor processor = decomposedSlices.get(tempIndex).duplicate();
                    stack.setProcessor(processor, getStackIndexInSingleChannel(image, z + 1, t + 1));
                }
            }

            ImagePlus output = new ImagePlus(image.getTitle() + " C=" + channelIndex, stack);
            dataBatch.addOutputData(slotName, new ImagePlusGreyscaleData(output), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    public int getStackIndexInSingleChannel(ImagePlus image, int slice, int frame) {
        int nSlices = image.getNSlices();
        int nFrames = image.getNFrames();
        int nChannels = 1;
        int channel = 1;
        if (slice < 1) slice = 1;
        if (slice > nSlices) slice = nSlices;
        if (frame < 1) frame = 1;
        if (frame > nFrames) frame = nFrames;
        return (frame - 1) * nChannels * nSlices + (slice - 1) * nChannels + channel;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (annotateWithChannelIndex && StringUtils.isNullOrEmpty(annotationColumnChannelIndex)) {
            report.resolve("Channel index annotation column").reportIsInvalid("Column name is empty!",
                    "You enabled adding the channel index as output annotation, but the column name is empty",
                    "Change the column name to a non-empty string",
                    this);
        }
        if (annotateWithSlotName && StringUtils.isNullOrEmpty(annotationColumnSlotName)) {
            report.resolve("Slot name annotation column").reportIsInvalid("Column name is empty!",
                    "You enabled adding the channel index as output annotation, but the column name is empty",
                    "Change the column name to a non-empty string",
                    this);
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

    @JIPipeDocumentation(name = "Annotate with slot names", description = "If enabled, the output slot name is added as annotation")
    @JIPipeParameter("annotate-with-slot-name")
    public boolean isAnnotateWithSlotName() {
        return annotateWithSlotName;
    }

    @JIPipeParameter("annotate-with-slot-name")
    public void setAnnotateWithSlotName(boolean annotateWithSlotName) {
        this.annotateWithSlotName = annotateWithSlotName;
    }

    @JIPipeDocumentation(name = "Annotate with channel-index", description = "If enabled, the output slot name is added as annotation")
    @JIPipeParameter("annotate-with-channel-index")
    public boolean isAnnotateWithChannelIndex() {
        return annotateWithChannelIndex;
    }

    @JIPipeParameter("annotate-with-channel-index")
    public void setAnnotateWithChannelIndex(boolean annotateWithChannelIndex) {
        this.annotateWithChannelIndex = annotateWithChannelIndex;
    }

    @JIPipeDocumentation(name = "Channel index annotation column", description = "The annotation name that is used if 'Annotate with channel index' is enabled")
    @JIPipeParameter("channel-index-annotation-column")
    public String getAnnotationColumnChannelIndex() {
        return annotationColumnChannelIndex;
    }

    @JIPipeParameter("channel-index-annotation-column")
    public void setAnnotationColumnChannelIndex(String annotationColumnChannelIndex) {
        this.annotationColumnChannelIndex = annotationColumnChannelIndex;
    }

    @JIPipeDocumentation(name = "Slot name annotation column", description = "The annotation name that is used if 'Annotate with slot names' is enabled")
    @JIPipeParameter("slot-name-annotation-column")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getAnnotationColumnSlotName() {
        return annotationColumnSlotName;
    }

    @JIPipeParameter("slot-name-annotation-column")
    public void setAnnotationColumnSlotName(String annotationColumnSlotName) {
        this.annotationColumnSlotName = annotationColumnSlotName;
    }
}
