package org.hkijena.acaq5.extension.algorithms.converters;

import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQMultichannelImageDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.datatypes.ACAQMultichannelImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQROIData;

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
