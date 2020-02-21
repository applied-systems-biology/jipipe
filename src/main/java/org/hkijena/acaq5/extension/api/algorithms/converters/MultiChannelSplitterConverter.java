package org.hkijena.acaq5.extension.api.algorithms.converters;

import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMultichannelImageDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;

@ACAQDocumentation(name = "Split multichannel image")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)
@AutoTransferTraits
public class MultiChannelSplitterConverter extends ACAQAlgorithm {

    public MultiChannelSplitterConverter() {
        super(ACAQMutableSlotConfiguration.builder()
                .addInputSlot("Multichannel image", ACAQMultichannelImageDataSlot.class)
                .sealInput()
                .restrictOutputTo(ACAQGreyscaleImageDataSlot.class)
                .build(), null);
    }

    public MultiChannelSplitterConverter(MultiChannelSplitterConverter other) {
        super(other);
    }

    @Override
    public void run() {
        ACAQMultichannelImageDataSlot inputSlot = (ACAQMultichannelImageDataSlot)getInputSlots().get(0);
        ImagePlus inputImage = inputSlot.getData().getImage();
        ImagePlus[] outputImages = ChannelSplitter.split(inputImage);
        int i = 0;
        for(ACAQDataSlot<?> slot : getOutputSlots()) {
            ACAQGreyscaleImageDataSlot outputSlot = (ACAQGreyscaleImageDataSlot)slot;
            outputSlot.setData(new ACAQGreyscaleImageData(outputImages[i++]));
        }
    }
}
