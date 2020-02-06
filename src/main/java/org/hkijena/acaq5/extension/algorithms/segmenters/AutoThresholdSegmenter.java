package org.hkijena.acaq5.extension.algorithms.segmenters;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.Thresholder;
import ij.process.AutoThresholder;
import org.hkijena.acaq5.api.ACAQInputDataSlot;
import org.hkijena.acaq5.api.ACAQOutputDataSlot;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

/**
 * Segmenter node that thresholds via an auto threshold
 */
public class AutoThresholdSegmenter extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

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
