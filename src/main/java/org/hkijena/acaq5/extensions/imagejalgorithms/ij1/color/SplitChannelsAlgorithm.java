package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color;

import ij.ImagePlus;
import ij.plugin.ChannelArranger;
import ij.plugin.ChannelSplitter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.acaq5.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ChannelArranger}
 */
@ACAQDocumentation(name = "Split channels", description = "Splits multichannel images into multiple greyscale images. " +
        "This operation is applied for each 2D image slice.")
@ACAQOrganization(menuPath = "Colors", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusColorData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output")
public class SplitChannelsAlgorithm extends ImageJ1Algorithm {

    private OutputSlotMapParameterCollection channelToSlotAssignment;
    private boolean ignoreMissingChannels = false;
    private ACAQTraitDeclarationRef annotationType = new ACAQTraitDeclarationRef(ACAQTraitRegistry.getInstance().getDeclarationById("image-index"));

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public SplitChannelsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
                .addInputSlot("Input", ImagePlusColorData.class)
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
        channelToSlotAssignment = new OutputSlotMapParameterCollection(Integer.class, this, () -> 0, false);
        other.channelToSlotAssignment.copyTo(channelToSlotAssignment);
        registerSubParameter(channelToSlotAssignment);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus image = dataInterface.getInputData(getFirstInputSlot(), ImagePlusColorData.class).getImage();
        ImagePlus[] slices = ChannelSplitter.split(image);
        for (Map.Entry<String, ACAQParameterAccess> entry : channelToSlotAssignment.getParameters().entrySet()) {
            String slotName = entry.getKey();
            int channelIndex = entry.getValue().get(Integer.class);

            if (channelIndex >= slices.length) {
                if (ignoreMissingChannels) {
                    continue;
                } else {
                    throw new UserFriendlyRuntimeException(new IndexOutOfBoundsException("Requested channel " + channelIndex + ", but only " + slices.length + " channels are available."),
                            "Could not find channel with index " + channelIndex,
                            "'Split channels' algorithm, slot '" + slotName + "'",
                            "You requested that the input channel " + channelIndex + " should be assigned to slot '" + slotName + "', but there are only " + slices.length + " channels available.",
                            "Please check if the index is correct. The first channel index is zero. You can also enable 'Ignore missing channels' to skip such occurrences silently.");
                }
            }

            List<ACAQTrait> annotations = new ArrayList<>();
            if (annotationType.getDeclaration() != null) {
                annotations.add(annotationType.getDeclaration().newInstance("channel=" + channelIndex));
            }

            dataInterface.addOutputData(slotName, new ImagePlusGreyscaleData(slices[channelIndex]), annotations);
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
    public ACAQTraitDeclarationRef getAnnotationType() {
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(ACAQTraitDeclarationRef annotationType) {
        this.annotationType = annotationType;
    }
}
