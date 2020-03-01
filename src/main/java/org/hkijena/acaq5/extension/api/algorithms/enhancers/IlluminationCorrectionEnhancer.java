package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.global.AutoTransferTraits;
import org.hkijena.acaq5.api.traits.global.GoodForTrait;
import org.hkijena.acaq5.api.traits.global.RemovesTrait;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.UnlabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.quality.NonUniformBrightnessQuality;
import org.hkijena.acaq5.utils.ImageJUtils;
import org.hkijena.acaq5.utils.MacroSetting;

@ACAQDocumentation(name = "Illumination correction enhancer")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQGreyscaleImageData.class, slotName = "Input image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQGreyscaleImageData.class, slotName = "Output image", autoCreate = true)

// Trait matching
@GoodForTrait(UnlabeledBioObjects.class)
@GoodForTrait(NonUniformBrightnessQuality.class)

// Trait configuration
@AutoTransferTraits
@RemovesTrait(NonUniformBrightnessQuality.class)
public class IlluminationCorrectionEnhancer extends ACAQIteratingAlgorithm {

    private int gaussianSigma = 21;

    public IlluminationCorrectionEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public IlluminationCorrectionEnhancer(IlluminationCorrectionEnhancer other) {
        super(other);
        this.gaussianSigma = other.gaussianSigma;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQGreyscaleImageData inputData = dataInterface.getInputData(getFirstInputSlot());
        ImagePlus img = inputData.getImage().duplicate();

        // Convert image to 32 bit
        ImageJUtils.runOnImage(img, "32-bit");

        // Estimate a background image
        ImagePlus gaussian = ImageJUtils.runOnNewImage(img, "Gaussian Blur...", new MacroSetting("sigma", gaussianSigma));
        ImageJUtils.runOnImage(gaussian, "Divide...", new MacroSetting("value", gaussian.getStatistics().max));

        // Divide by background
        ImageCalculator calculator = new ImageCalculator();
        ImagePlus result = calculator.run("Divide create 32-bit", img, gaussian);

        dataInterface.addOutputData(getFirstOutputSlot(), new ACAQGreyscaleImageData(result));
    }

    @ACAQParameter("gaussian-sigma")
    @ACAQDocumentation(name = "Gaussian sigma")
    public int getGaussianSigma() {
        return gaussianSigma;
    }

    @ACAQParameter("gaussian-sigma")
    public void setGaussianSigma(int gaussianSigma) {
        this.gaussianSigma = gaussianSigma;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
