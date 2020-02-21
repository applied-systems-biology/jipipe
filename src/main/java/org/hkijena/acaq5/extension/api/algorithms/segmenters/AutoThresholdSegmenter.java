package org.hkijena.acaq5.extension.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.Thresholder;
import ij.process.AutoThresholder;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.*;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.quality.ImageQuality;
import org.hkijena.acaq5.extension.api.traits.quality.NonUniformBrightnessQuality;
import org.hkijena.acaq5.extension.api.traits.bioobject.count.ClusterBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.BioObjectsPreparations;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.LabeledBioObjects;
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
@RemovesTrait(BioObjectsPreparations.class)
@AddsTrait(ClusterBioObjects.class)
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
