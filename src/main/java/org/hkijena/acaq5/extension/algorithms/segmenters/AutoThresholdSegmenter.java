package org.hkijena.acaq5.extension.algorithms.segmenters;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.Thresholder;
import ij.process.AutoThresholder;
import org.hkijena.acaq5.api.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.ACAQAlgorithmMetadata;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

/**
 * Segmenter node that thresholds via an auto threshold
 */
@ACAQDocumentation(name = "Auto threshold segmentation")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Segmenter)
public class AutoThresholdSegmenter extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

    private String method = AutoThresholder.getMethods()[0];

    public AutoThresholdSegmenter() {
        super("Image", ACAQGreyscaleImageDataSlot.class,
                "Mask", ACAQMaskDataSlot.class);
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
