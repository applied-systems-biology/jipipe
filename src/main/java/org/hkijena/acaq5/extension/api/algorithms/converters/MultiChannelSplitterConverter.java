package org.hkijena.acaq5.extension.api.algorithms.converters;

import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.AutoTransferTraits;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;

// Algorithm metadata
@ACAQDocumentation(name = "Split multichannel image")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)

//Algorithm data flow
@AlgorithmInputSlot(ACAQMultichannelImageData.class)
@AlgorithmOutputSlot(ACAQGreyscaleImageData.class)

// Algorithm traits
@AutoTransferTraits
public class MultiChannelSplitterConverter extends ACAQIteratingAlgorithm {

    public MultiChannelSplitterConverter(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
                .addInputSlot("Multichannel image", ACAQMultichannelImageData.class)
                .sealInput()
                .restrictOutputTo(ACAQGreyscaleImageData.class)
                .build(), null);
    }

    public MultiChannelSplitterConverter(MultiChannelSplitterConverter other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQMultichannelImageData inputData = dataInterface.getInputData(getFirstInputSlot());
        ImagePlus[] outputImages = ChannelSplitter.split(inputData.getImage());
        int i = 0;
        for (ACAQDataSlot slot : getOutputSlots()) {
            dataInterface.addOutputData(slot, new ACAQGreyscaleImageData(outputImages[i++]));
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
