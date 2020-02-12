package org.hkijena.acaq5.extension.api.algorithms.converters;

import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMultichannelImageDataSlot;

@ACAQDocumentation(name = "Split multichannel image")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)
public class MultiChannelSplitterConverter extends ACAQAlgorithm {

    public MultiChannelSplitterConverter() {
        super(ACAQMutableSlotConfiguration.builder()
                .addInputSlot("Multichannel image", ACAQMultichannelImageDataSlot.class)
                .sealInput()
                .restrictOutputTo(ACAQGreyscaleImageDataSlot.class)
                .build());
    }

    @Override
    public void run() {
    }
}
