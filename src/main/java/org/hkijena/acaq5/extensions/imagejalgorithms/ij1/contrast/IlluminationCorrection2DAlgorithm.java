package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.contrast;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.GaussianBlur2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies illumination correction
 */
@ACAQDocumentation(name = "Illumination correction 2D",
        description = "Applies a Gaussian filter to the image and extracts the maximum value. Pixel values are then divided by this value." +
                "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Contrast")

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output")

// Trait matching
@GoodForTrait("bioobject-preparations-labeling-unlabeled")
@GoodForTrait("image-quality-brightness-nonuniform")

// Trait configuration
@RemovesTrait("image-quality-brightness-nonuniform")
public class IlluminationCorrection2DAlgorithm extends ACAQIteratingAlgorithm {

    private GaussianBlur2DAlgorithm gaussianAlgorithm = ACAQAlgorithm.newInstance("ij1-blur-gaussian2d");

    /**
     * @param declaration the algorithm declaration
     */
    public IlluminationCorrection2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale32FData.class)
                .addOutputSlot("Output", ImagePlusGreyscale32FData.class, null)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        gaussianAlgorithm.setSigmaX(21);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public IlluminationCorrection2DAlgorithm(IlluminationCorrection2DAlgorithm other) {
        super(other);
        this.gaussianAlgorithm = (GaussianBlur2DAlgorithm) other.gaussianAlgorithm.duplicate();
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusGreyscale32FData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class);

        gaussianAlgorithm.clearSlotData();
        gaussianAlgorithm.getFirstInputSlot().addData(inputData);
        gaussianAlgorithm.run(subProgress.resolve("Gaussian"), algorithmProgress, isCancelled);
        ImagePlus background = gaussianAlgorithm.getFirstOutputSlot().getData(0, ImagePlusGreyscale32FData.class).getImage();

        ImageJUtils.forEachSlice(background, imp -> {
            double max = imp.getStatistics().max;
            imp.multiply(1.0 / max);
        });

        ImageCalculator calculator = new ImageCalculator();
        ImagePlus result = calculator.run("Divide stack create 32-bit", inputData.getImage(), background);

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }

    @ACAQDocumentation(name = "Gaussian filter")
    @ACAQParameter(value = "gaussian-algorithm", visibility = ACAQParameterVisibility.TransitiveVisible)
    public GaussianBlur2DAlgorithm getGaussianAlgorithm() {
        return gaussianAlgorithm;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Gaussian filter").report(gaussianAlgorithm);
    }
}
