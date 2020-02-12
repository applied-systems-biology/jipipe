package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.utils.ImageJUtils;
import org.hkijena.acaq5.utils.MacroSetting;

@ACAQDocumentation(name = "Illumination correction enhancer")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)
public class IlluminationCorrectionEnhancer extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQGreyscaleImageData> {

    private int gaussianSigma = 21;

    public IlluminationCorrectionEnhancer() {
        super("Input image", ACAQGreyscaleImageDataSlot.class,
                "Output image", ACAQGreyscaleImageDataSlot.class);
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();

        // Convert image to 32 bit
        ImageJUtils.runOnImage(img, "32-bit");

        // Estimate a background image
        ImagePlus gaussian = ImageJUtils.runOnNewImage(img, "Gaussian Blur...", new MacroSetting("sigma", gaussianSigma));
        ImageJUtils.runOnImage(gaussian, "Divide...",new MacroSetting("value", gaussian.getStatistics().max));

        // Divide by background
        ImageCalculator calculator = new ImageCalculator();
        ImagePlus result = calculator.run("Divide create 32-bit", img, gaussian);

        getOutputSlot().setData(new ACAQGreyscaleImageData(result));
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
}
