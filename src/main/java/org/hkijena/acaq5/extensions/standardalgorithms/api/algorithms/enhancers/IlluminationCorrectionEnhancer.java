package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.UnlabeledBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.quality.NonUniformBrightnessQuality;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.acaq5.utils.ImageJUtils;
import org.hkijena.acaq5.utils.MacroSetting;

@ACAQDocumentation(name = "Illumination correction enhancer")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlus2DGreyscaleData.class, slotName = "Input image", autoCreate = true)
@AlgorithmOutputSlot(value = ImagePlus2DGreyscaleData.class, slotName = "Output image", autoCreate = true)

// Trait matching
@GoodForTrait(UnlabeledBioObjects.class)
@GoodForTrait(NonUniformBrightnessQuality.class)

// Trait configuration
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
        ImagePlus2DGreyscaleData inputData = dataInterface.getInputData(getFirstInputSlot());
        ImagePlus img = inputData.getImage().duplicate();

        // Convert image to 32 bit
        ImageJUtils.runOnImage(img, "32-bit");

        // Estimate a background image
        ImagePlus gaussian = ImageJUtils.runOnNewImage(img, "Gaussian Blur...", new MacroSetting("sigma", gaussianSigma));
        ImageJUtils.runOnImage(gaussian, "Divide...", new MacroSetting("value", gaussian.getStatistics().max));

        // Divide by background
        ImageCalculator calculator = new ImageCalculator();
        ImagePlus result = calculator.run("Divide create 32-bit", img, gaussian);

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleData(result));
    }

    @ACAQParameter("gaussian-sigma")
    @ACAQDocumentation(name = "Gaussian sigma")
    public int getGaussianSigma() {
        return gaussianSigma;
    }

    @ACAQParameter("gaussian-sigma")
    public void setGaussianSigma(int gaussianSigma) {
        this.gaussianSigma = gaussianSigma;
        getEventBus().post(new ParameterChangedEvent(this, "gaussian-sigma"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
