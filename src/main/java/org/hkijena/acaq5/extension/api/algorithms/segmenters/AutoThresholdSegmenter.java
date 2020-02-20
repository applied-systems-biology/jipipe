package org.hkijena.acaq5.extension.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.Thresholder;
import ij.process.AutoThresholder;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.traits.BadForTrait;
import org.hkijena.acaq5.api.traits.GoodForTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.ImageQuality;
import org.hkijena.acaq5.extension.api.traits.NonUniformBrightnessQuality;
import org.hkijena.acaq5.extension.api.traits.bioobject.ClusterBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.LabeledBioObjects;
import org.hkijena.acaq5.utils.ImageJUtils;

/**
 * Segmenter node that thresholds via an auto threshold
 */
@ACAQDocumentation(name = "Auto threshold segmentation")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Segmenter)

// Trait matching
@GoodForTrait(LabeledBioObjects.class)
@BadForTrait(NonUniformBrightnessQuality.class)

// Trait configuration
@AutoTransferTraits
@RemovesTrait(ImageQuality.class)
public class AutoThresholdSegmenter extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

    private AutoThresholder.Method method = AutoThresholder.Method.Default;

    public AutoThresholdSegmenter() {
        super("Image", ACAQGreyscaleImageDataSlot.class,
                "Mask", ACAQMaskDataSlot.class);
    }

    public AutoThresholdSegmenter(AutoThresholdSegmenter other) {
        super(other);
        this.method = other.method;
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();
        ImagePlus result = img.duplicate();

        Thresholder.setMethod(method.name());
        Prefs.blackBackground = true;
        ImageJUtils.runOnImage(result, new Thresholder());

        getOutputSlot().setData(new ACAQMaskData(result));
    }

    @ACAQParameter("method")
    @ACAQDocumentation(name = "Method")
    public AutoThresholder.Method getMethod() {
        return method;
    }

    @ACAQParameter("method")
    public void setMethod(AutoThresholder.Method method) {
        this.method = method;
    }
}
