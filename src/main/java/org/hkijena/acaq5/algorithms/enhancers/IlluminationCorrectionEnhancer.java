package org.hkijena.acaq5.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import org.hkijena.acaq5.ACAQInputDataSlot;
import org.hkijena.acaq5.ACAQOutputDataSlot;
import org.hkijena.acaq5.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.utils.ImageJUtils;
import org.hkijena.acaq5.utils.MacroFlag;
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
