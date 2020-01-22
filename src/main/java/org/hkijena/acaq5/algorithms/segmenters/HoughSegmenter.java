package org.hkijena.acaq5.algorithms.segmenters;

import org.hkijena.acaq5.ACAQAlgorithm;
import org.hkijena.acaq5.ACAQInputDataSlot;
import org.hkijena.acaq5.ACAQOutputDataSlot;
import org.hkijena.acaq5.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.datatypes.ACAQMaskData;

public class HoughSegmenter extends ACAQSimpleAlgorithm<ACAQInputDataSlot<ACAQGreyscaleImageData>,
        ACAQOutputDataSlot<ACAQMaskData>> {

    public HoughSegmenter() {
        super(new ACAQInputDataSlot<>("Image", ACAQGreyscaleImageData.class),
                new ACAQOutputDataSlot<>("Mask", ACAQMaskData.class));
    }

    @Override
    public void run() {

    }
}