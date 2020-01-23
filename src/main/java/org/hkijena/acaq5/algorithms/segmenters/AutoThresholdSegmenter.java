package org.hkijena.acaq5.algorithms.segmenters;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.Thresholder;
import ij.process.AutoThresholder;
import org.hkijena.acaq5.ACAQInputDataSlot;
import org.hkijena.acaq5.ACAQOutputDataSlot;
import org.hkijena.acaq5.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.datatypes.ACAQMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

/**
 * Segmenter node that thresholds via an auto threshold
 */
public class AutoThresholdSegmenter extends ACAQSimpleAlgorithm<ACAQInputDataSlot<ACAQGreyscaleImageData>,
        ACAQOutputDataSlot<ACAQMaskData>> {

    private String method = AutoThresholder.getMethods()[0];

    public AutoThresholdSegmenter() {
        super(new ACAQInputDataSlot<>("Image", ACAQGreyscaleImageData.class),
                new ACAQOutputDataSlot<>("Mask", ACAQMaskData.class));
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();
        ImagePlus result = img.duplicate();

        Thresholder.setMethod(method);
        Prefs.blackBackground = true;
        ImageJUtils.runOnImage(result, new Thresholder());

        getOutputSlot().setData(new ACAQMaskData(result));
    }
}
