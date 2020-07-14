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
import ij.ImageStack;
import ij.plugin.ChannelArranger;
import ij.plugin.ChannelSplitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.SliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ChannelArranger}
 */
@JIPipeDocumentation(name = "Split channels", description = "Splits multichannel images into multiple greyscale images. " +
        "This operation is applied for each 2D image slice.")
@JIPipeOrganization(menuPath = "Colors", algorithmCategory = JIPipeNodeCategory.Processor)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output")
public class SplitChannelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OutputSlotMapParameterCollection channelToSlotAssignment;
    private boolean ignoreMissingChannels = false;
    private String annotationColumnSlotName = "Channel";
    private String annotationColumnChannelIndex = "Channel index";
    private boolean annotateWithChannelIndex = true;
    private boolean annotateWithSlotName = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public SplitChannelsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", ImagePlusData.class)
                .restrictOutputTo(ImageJDataTypesExtension.IMAGE_TYPES_GREYSCALE)
                .allowOutputSlotInheritance(false)
                .sealInput()
                .build());
        channelToSlotAssignment = new OutputSlotMapParameterCollection(Integer.class, this, () -> 0, false);
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

        channelToSlotAssignment = new OutputSlotMapParameterCollection(Integer.class, this, () -> 0, false);
        other.channelToSlotAssignment.copyTo(channelToSlotAssignment);
        registerSubParameter(channelToSlotAssignment);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus image = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class).getImage();

        // If we have a grayscale image then we can just skip everything
        if (!image.isComposite() && image.getType() != ImagePlus.COLOR_256 && image.getType() != ImagePlus.COLOR_RGB) {
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

                List<JIPipeAnnotation> annotations = new ArrayList<>();
                if (annotateWithSlotName) {
                    annotations.add(new JIPipeAnnotation(annotationColumnSlotName, slotName));
                }
                if(annotateWithChannelIndex) {
                    annotations.add(new JIPipeAnnotation(annotationColumnChannelIndex, "" + channelIndex));
                }
                dataInterface.addOutputData(slotName, new ImagePlusGreyscaleData(image), annotations);
            }
            return;
        }

        // First, we need to ensure that we only have 2D grayscale planes
        // This means we have to completely decompose the image
        Map<SliceIndex, ImageProcessor> decomposedSlices = new HashMap<>();
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
                decomposedSlices.put(new SliceIndex(sliceIndex.getZ(), entry.getKey(), sliceIndex.getT()), entry.getValue());
            }
        });
        int nChannels = decomposedSlices.keySet().stream().map(SliceIndex::getC).max(Comparator.naturalOrder()).orElse(-1) + 1;

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

            List<JIPipeAnnotation> annotations = new ArrayList<>();
            if (annotateWithSlotName) {
                annotations.add(new JIPipeAnnotation(annotationColumnSlotName, slotName));
            }
            if(annotateWithChannelIndex) {
                annotations.add(new JIPipeAnnotation(annotationColumnChannelIndex, "" + channelIndex));
            }

            // Rebuild image stack
            ImageStack stack = new ImageStack(image.getWidth(), image.getHeight(), image.getNSlices() * image.getNFrames());
            SliceIndex tempIndex = new SliceIndex();
            tempIndex.setC(channelIndex);
            for (int t = 0; t < image.getNFrames(); t++) {
                tempIndex.setT(t);
                for (int z = 0; z < image.getNSlices(); z++) {
                    tempIndex.setZ(z);
                    ImageProcessor processor = decomposedSlices.get(tempIndex).duplicate();
                    stack.setProcessor(processor, image.getStackIndex(1, z + 1, t + 1));
                }
            }

            ImagePlus output = new ImagePlus(image.getTitle() + " C=" + channelIndex, stack);
            dataInterface.addOutputData(slotName, new ImagePlusGreyscaleData(output), annotations);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if(annotateWithChannelIndex && StringUtils.isNullOrEmpty(annotationColumnChannelIndex)) {
            report.forCategory("Channel index annotation column").reportIsInvalid("Column name is empty!",
                    "You enabled adding the channel index as output annotation, but the column name is empty",
                    "Change the column name to a non-empty string",
                    this);
        }
        if(annotateWithSlotName && StringUtils.isNullOrEmpty(annotationColumnSlotName)) {
            report.forCategory("Slot name annotation column").reportIsInvalid("Column name is empty!",
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
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getAnnotationColumnSlotName() {
        return annotationColumnSlotName;
    }

    @JIPipeParameter("slot-name-annotation-column")
    public void setAnnotationColumnSlotName(String annotationColumnSlotName) {
        this.annotationColumnSlotName = annotationColumnSlotName;
    }
}
