package org.hkijena.acaq5.extension.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.Thresholder;
import ij.process.AutoThresholder;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.traits.AddsTrait;
import org.hkijena.acaq5.api.data.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.data.traits.BadForTrait;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.bioobject.count.ClusterBioObjects;
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
@AlgorithmInputSlot(value = ACAQGreyscaleImageData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMaskData.class, slotName = "Mask", autoCreate = true)

// Trait matching
@GoodForTrait(UniformlyLabeledBioObjects.class)
@BadForTrait(NonUniformBrightnessQuality.class)

// Trait configuration
@AutoTransferTraits
@RemovesTrait(ImageQuality.class)
@AddsTrait(ClusterBioObjects.class)
public class AutoThresholdSegmenter extends ACAQIteratingAlgorithm {

    private AutoThresholder.Method method = AutoThresholder.Method.Default;

    public AutoThresholdSegmenter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public AutoThresholdSegmenter(AutoThresholdSegmenter other) {
        super(other);
        this.method = other.method;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQGreyscaleImageData inputData = dataInterface.getInputData(getFirstInputSlot());
        ImagePlus img = inputData.getImage();
        ImagePlus result = img.duplicate();

        Thresholder.setMethod(method.name());
        Prefs.blackBackground = true;
        ImageJUtils.runOnImage(result, new Thresholder());

        dataInterface.addOutputData(getFirstOutputSlot(), new ACAQMaskData(result));
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
