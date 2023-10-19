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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.contrast;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.blur.GaussianBlur2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Applies illumination correction
 */
@JIPipeDocumentation(name = "Illumination correction 2D",
        description = "Applies a Gaussian filter to the image and extracts the maximum value. Pixel values are then divided by this value." +
                "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Contrast")
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", autoCreate = true)
public class IlluminationCorrection2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private GaussianBlur2DAlgorithm gaussianAlgorithm =
            JIPipe.createNode("ij1-blur-gaussian2d");

    /**
     * @param info the algorithm info
     */
    public IlluminationCorrection2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlusGreyscale32FData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo);

        GaussianBlur2DAlgorithm gaussianAlgorithmCopy = new GaussianBlur2DAlgorithm(gaussianAlgorithm);
        gaussianAlgorithmCopy.getFirstInputSlot().addData(inputData, progressInfo);
        gaussianAlgorithmCopy.run(progressInfo);
        ImagePlus background = gaussianAlgorithmCopy.getFirstOutputSlot().getData(0, ImagePlusGreyscale32FData.class, progressInfo).getImage();

        ImageJUtils.forEachSlice(background, imp -> {
            double max = imp.getStatistics().max;
            imp.multiply(1.0 / max);
        }, progressInfo);

        ImageCalculator calculator = new ImageCalculator();
        ImagePlus result = calculator.run("Divide stack create 32-bit", inputData.getImage(), background);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @JIPipeDocumentation(name = "Gaussian filter")
    @JIPipeParameter(value = "gaussian-algorithm")
    public GaussianBlur2DAlgorithm getGaussianAlgorithm() {
        return gaussianAlgorithm;
    }
}
