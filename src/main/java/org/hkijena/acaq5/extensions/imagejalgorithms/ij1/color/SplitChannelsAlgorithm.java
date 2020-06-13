package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelArranger;
import ij.plugin.ChannelSplitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.imagejalgorithms.SliceIndex;
import org.hkijena.acaq5.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.acaq5.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ImageJUtils;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ChannelArranger}
 */
@ACAQDocumentation(name = "Split channels", description = "Splits multichannel images into multiple greyscale images. " +
        "This operation is applied for each 2D image slice.")
@ACAQOrganization(menuPath = "Colors", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output")
public class SplitChannelsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private OutputSlotMapParameterCollection channelToSlotAssignment;
    private boolean ignoreMissingChannels = false;
    private String annotationType = "Image index";

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public SplitChannelsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
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
        this.annotationType = other.annotationType;
        this.ignoreMissingChannels = other.ignoreMissingChannels;

        channelToSlotAssignment = new OutputSlotMapParameterCollection(Integer.class, this, () -> 0, false);
        other.channelToSlotAssignment.copyTo(channelToSlotAssignment);
        registerSubParameter(channelToSlotAssignment);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus image = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class).getImage();

        // If we have a grayscale image then we can just skip everything
        if (!image.isComposite() && image.getType() != ImagePlus.COLOR_256 && image.getType() != ImagePlus.COLOR_RGB) {
            int nChannels = 1;
            for (Map.Entry<String, ACAQParameterAccess> entry : channelToSlotAssignment.getParameters().entrySet()) {
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

                List<ACAQAnnotation> annotations = new ArrayList<>();
                if (!StringUtils.isNullOrEmpty(annotationType)) {
                    annotations.add(new ACAQAnnotation(annotationType, "channel=" + channelIndex));
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

        for (Map.Entry<String, ACAQParameterAccess> entry : channelToSlotAssignment.getParameters().entrySet()) {
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

            List<ACAQAnnotation> annotations = new ArrayList<>();
            if (!StringUtils.isNullOrEmpty(annotationType)) {
                annotations.add(new ACAQAnnotation(annotationType, "channel=" + channelIndex));
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
    public void reportValidity(ACAQValidityReport report) {

    }

    @ACAQDocumentation(name = "Channel assignment", description = "Please create an output slot for each greyscale channel you " +
            "want to extract. Then assign the source channel index. The first index is zero.")
    @ACAQParameter("channel-to-slot-assignments")
    public OutputSlotMapParameterCollection getChannelToSlotAssignment() {
        return channelToSlotAssignment;
    }

    @ACAQDocumentation(name = "Ignore missing channels", description = "If enabled, the algorithm silently skips invalid assignments like extracting the 4th channel of a 2-channel image. " +
            "If disabled, an error will be thrown if such a condition is detected.")
    @ACAQParameter("ignore-missing-channels")
    public boolean isIgnoreMissingChannels() {
        return ignoreMissingChannels;
    }

    @ACAQParameter("ignore-missing-channels")
    public void setIgnoreMissingChannels(boolean ignoreMissingChannels) {
        this.ignoreMissingChannels = ignoreMissingChannels;
    }

    @ACAQDocumentation(name = "Generated annotation", description = "An optional annotation that is generated for each output to indicate which channel the data is coming from. " +
            "The format will be channel=[index].")
    @ACAQParameter("annotation-type")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getAnnotationType() {
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }
}
