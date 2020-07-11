/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.contrast;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.GaussianBlur2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies illumination correction
 */
@JIPipeDocumentation(name = "Illumination correction 2D",
        description = "Applies a Gaussian filter to the image and extracts the maximum value. Pixel values are then divided by this value." +
                "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Contrast")

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output")

// Trait matching

// Trait configuration
public class IlluminationCorrection2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private GaussianBlur2DAlgorithm gaussianAlgorithm = JIPipeGraphNode.newInstance("ij1-blur-gaussian2d");

    /**
     * @param declaration the algorithm declaration
     */
    public IlluminationCorrection2DAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale32FData.class)
                .addOutputSlot("Output", ImagePlusGreyscale32FData.class, null)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        gaussianAlgorithm.setSigmaX(20);
        gaussianAlgorithm.setSigmaY(20);
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
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataInterface dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusGreyscale32FData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class);

        GaussianBlur2DAlgorithm gaussianAlgorithmCopy = new GaussianBlur2DAlgorithm(gaussianAlgorithm);
        gaussianAlgorithmCopy.getFirstInputSlot().addData(inputData);
        gaussianAlgorithmCopy.run(subProgress.resolve("Gaussian"), algorithmProgress, isCancelled);
        ImagePlus background = gaussianAlgorithmCopy.getFirstOutputSlot().getData(0, ImagePlusGreyscale32FData.class).getImage();

        ImageJUtils.forEachSlice(background, imp -> {
            double max = imp.getStatistics().max;
            imp.multiply(1.0 / max);
        });

        ImageCalculator calculator = new ImageCalculator();
        ImagePlus result = calculator.run("Divide stack create 32-bit", inputData.getImage(), background);

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }

    @JIPipeDocumentation(name = "Gaussian filter")
    @JIPipeParameter(value = "gaussian-algorithm", visibility = JIPipeParameterVisibility.TransitiveVisible, uiExcludeSubParameters = {"jipipe:data-batch-generation", "jipipe:parameter-slot-algorithm"})
    public GaussianBlur2DAlgorithm getGaussianAlgorithm() {
        return gaussianAlgorithm;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Gaussian filter").report(gaussianAlgorithm);
    }
}
