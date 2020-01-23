package org.hkijena.acaq5.extension.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import org.hkijena.acaq5.api.ACAQInputDataSlot;
import org.hkijena.acaq5.api.ACAQOutputDataSlot;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.utils.ImageJUtils;
import org.hkijena.acaq5.utils.MacroSetting;

public class IlluminationCorrectionEnhancer extends ACAQSimpleAlgorithm<ACAQInputDataSlot<ACAQGreyscaleImageData>,
        ACAQOutputDataSlot<ACAQGreyscaleImageData>> {

    private int gaussianSigma = 21;

    public IlluminationCorrectionEnhancer() {
        super(new ACAQInputDataSlot<>("Input image", ACAQGreyscaleImageData.class),
                new ACAQOutputDataSlot<>("Output image", ACAQGreyscaleImageData.class));
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
}
