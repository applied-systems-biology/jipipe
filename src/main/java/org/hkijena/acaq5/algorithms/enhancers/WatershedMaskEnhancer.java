package org.hkijena.acaq5.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.filter.EDM;
import org.hkijena.acaq5.ACAQInputDataSlot;
import org.hkijena.acaq5.ACAQOutputDataSlot;
import org.hkijena.acaq5.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.datatypes.ACAQMaskData;

public class WatershedMaskEnhancer extends ACAQSimpleAlgorithm<ACAQInputDataSlot<ACAQMaskData>,
        ACAQOutputDataSlot<ACAQMaskData>> {

    public WatershedMaskEnhancer() {
        super(new ACAQInputDataSlot<>("Input image", ACAQMaskData.class),
                new ACAQOutputDataSlot<>("Output image", ACAQMaskData.class));
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getMask();

        EDM watershed = new EDM();
        ImagePlus result = img.duplicate();
        watershed.toWatershed(result.getProcessor());

        getOutputSlot().setData(new ACAQMaskData(result));
    }
}