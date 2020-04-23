package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.converters;

import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ConfigTraits;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Splits a color image's channels
 */
// Algorithm metadata
@ACAQDocumentation(name = "Split multichannel image")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)

//Algorithm data flow
@AlgorithmInputSlot(ImagePlus2DColorData.class)
@AlgorithmOutputSlot(ImagePlus2DGreyscaleData.class)

// Algorithm traits
@ConfigTraits(allowModify = true)
public class MultiChannelSplitterConverter extends ACAQIteratingAlgorithm {

    /**
     * @param declaration algorithm declaration
     */
    public MultiChannelSplitterConverter(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
                .addInputSlot("Multichannel image", ImagePlusData.class)
                .sealInput()
                .restrictOutputTo(ImagePlus2DGreyscaleData.class)
                .build(), null);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public MultiChannelSplitterConverter(MultiChannelSplitterConverter other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus[] outputImages = ChannelSplitter.split(inputData.getImage());
        int i = 0;
        for (ACAQDataSlot slot : getOutputSlots()) {
            dataInterface.addOutputData(slot, new ImagePlus2DGreyscaleData(outputImages[i++]));
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
