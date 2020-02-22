package org.hkijena.acaq5.extension.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.Thresholder;
import ij.process.AutoThresholder;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.*;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.bioobject.count.ClusterBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.BioObjectsPreparations;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.BioObjectsLabeling;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.UniformlyLabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.quality.ImageQuality;
import org.hkijena.acaq5.extension.api.traits.quality.NonUniformBrightnessQuality;
import org.hkijena.acaq5.utils.ImageJUtils;

/**
 * Segmenter node that thresholds via an auto threshold
 */
@ACAQDocumentation(name = "Auto threshold segmentation")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Segmenter)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQGreyscaleImageDataSlot.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMaskDataSlot.class, slotName = "Mask", autoCreate = true)

// Trait matching
@GoodForTrait(UniformlyLabeledBioObjects.class)
@BadForTrait(NonUniformBrightnessQuality.class)

// Trait configuration
@AutoTransferTraits
@RemovesTrait(ImageQuality.class)
@RemovesTrait(BioObjectsPreparations.class)
@RemovesTrait(BioObjectsLabeling.class)
@AddsTrait(ClusterBioObjects.class)
public class AutoThresholdSegmenter extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

    private AutoThresholder.Method method = AutoThresholder.Method.Default;

    public AutoThresholdSegmenter() {
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

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
