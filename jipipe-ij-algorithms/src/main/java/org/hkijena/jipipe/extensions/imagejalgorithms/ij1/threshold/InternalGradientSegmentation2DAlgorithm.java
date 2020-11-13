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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.contrast.CLAHEContrastEnhancer;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Internal gradient segmenter
 */
@JIPipeDocumentation(name = "Internal gradient segmentation 2D", description = "Segments objects by finding the internal gradients. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)


@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")


public class InternalGradientSegmentation2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double gaussSigma = 3;
    private int internalGradientRadius = 25;
    private int dilationIterations = 3;
    private int erosionIterations = 2;
    private boolean applyFirstCLAHE = true;
    private boolean applySecondCLAHE = true;
    private boolean applyGaussian = true;

    private AutoThreshold2DAlgorithm autoThresholding;
    private CLAHEContrastEnhancer contrastEnhancer;

    /**
     * @param info the algorithm info
     */
    public InternalGradientSegmentation2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        this.contrastEnhancer = (CLAHEContrastEnhancer) JIPipe.getNodes().getInfoById("ij1-contrast-clahe").newInstance();
        this.autoThresholding = (AutoThreshold2DAlgorithm) JIPipe.getNodes().getInfoById("ij1-threshold-auto2d").newInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public InternalGradientSegmentation2DAlgorithm(InternalGradientSegmentation2DAlgorithm other) {
        super(other);
        this.gaussSigma = other.gaussSigma;
        this.internalGradientRadius = other.internalGradientRadius;
        this.dilationIterations = other.dilationIterations;
        this.erosionIterations = other.erosionIterations;
        this.autoThresholding = (AutoThreshold2DAlgorithm) other.autoThresholding.getInfo().duplicate(other.autoThresholding);
        this.contrastEnhancer = (CLAHEContrastEnhancer) other.contrastEnhancer.getInfo().duplicate(other.contrastEnhancer);
        this.applyFirstCLAHE = other.applyFirstCLAHE;
        this.applySecondCLAHE = other.applySecondCLAHE;
        this.applyGaussian = other.applyGaussian;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    private void applyInternalGradient(ImagePlus img) {
        // Erode the original image
        ImagePlus eroded = img.duplicate();
        RankFilters erosionFilter = new RankFilters();
        erosionFilter.rank(eroded.getProcessor(), internalGradientRadius, RankFilters.MIN); //TODO: Set element to octagon

        // Apply image calculator
        ImageCalculator calculator = new ImageCalculator();
        calculator.run("Subtract", img, eroded);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnableInfo progress) {

        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());

        CLAHEContrastEnhancer contrastEnhancerCopy = new CLAHEContrastEnhancer(contrastEnhancer);
        AutoThreshold2DAlgorithm autoThresholdingCopy = new AutoThreshold2DAlgorithm(autoThresholding);

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            ImagePlus slice = new ImagePlus("slice", imp);
            ImagePlus processedSlice = slice.duplicate();

            // Apply CLAHE enhancer
            if (applyFirstCLAHE) {
                contrastEnhancerCopy.clearSlotData();
                contrastEnhancerCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice));
                contrastEnhancerCopy.run(progress);
                processedSlice = contrastEnhancerCopy.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();
            }

            if (applyGaussian) {
                (new GaussianBlur()).blurGaussian(processedSlice.getProcessor(), gaussSigma);
                ImageJUtils.runOnImage(processedSlice, "8-bit");
                applyInternalGradient(processedSlice);
            }

            if (applySecondCLAHE) {
                contrastEnhancerCopy.clearSlotData();
                contrastEnhancerCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice));
                contrastEnhancerCopy.run(progress);
                processedSlice = contrastEnhancerCopy.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();
            }

            // Convert image to mask and threshold with given auto threshold method
            autoThresholdingCopy.clearSlotData();
            autoThresholdingCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice));
            autoThresholdingCopy.run(progress);
            processedSlice = autoThresholdingCopy.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();

            // Apply set of rank filters
            Binary binaryFilter = new Binary();

            binaryFilter.setup("dilate", null);
            binaryFilter.run(processedSlice.getProcessor());

            binaryFilter.setup("fill holes", null);
            binaryFilter.run(processedSlice.getProcessor());

            binaryFilter.setup("dilate", null);
            for (int i = 0; i < dilationIterations; ++i) {
                binaryFilter.run(processedSlice.getProcessor());
            }

            binaryFilter.setup("fill holes", null);
            binaryFilter.run(processedSlice.getProcessor());

            binaryFilter.setup("erode", null);
            for (int i = 0; i < erosionIterations; ++i) {
                binaryFilter.run(processedSlice.getProcessor());
            }
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        });
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result));
    }

    @JIPipeParameter("gauss-sigma")
    @JIPipeDocumentation(name = "Gauss sigma", description = "Standard deviation of the Gaussian (pixels)")
    public double getGaussSigma() {
        return gaussSigma;
    }

    @JIPipeParameter("gauss-sigma")
    public void setGaussSigma(double gaussSigma) {
        this.gaussSigma = gaussSigma;

    }

    @JIPipeParameter("internal-gradient-radius")
    @JIPipeDocumentation(name = "Internal gradient radius", description = "Radius for the internal gradient radius calculation")
    public int getInternalGradientRadius() {
        return internalGradientRadius;
    }

    @JIPipeParameter("internal-gradient-radius")
    public void setInternalGradientRadius(int internalGradientRadius) {
        this.internalGradientRadius = internalGradientRadius;

    }

    @JIPipeParameter("dilation-iterations")
    @JIPipeDocumentation(name = "Dilation iterations", description = "Number of dilation iterations after filling holes")
    public int getDilationIterations() {
        return dilationIterations;
    }

    @JIPipeParameter("dilation-iterations")
    public void setDilationIterations(int dilationIterations) {
        this.dilationIterations = dilationIterations;

    }

    @JIPipeParameter("erosion-iterations")
    @JIPipeDocumentation(name = "Erosion iterations", description = "Number of erosion iterations after filling holes. If you do not want to change " +
            "the object sizes, keep this the same as the dilation iterations.")
    public int getErosionIterations() {
        return erosionIterations;
    }

    @JIPipeParameter("erosion-iterations")
    public void setErosionIterations(int erosionIterations) {
        this.erosionIterations = erosionIterations;

    }

    @JIPipeParameter(value = "auto-thresholding", uiExcludeSubParameters = {"jipipe:data-batch-generation", "jipipe:parameter-slot-algorithm"})
    @JIPipeDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThreshold2DAlgorithm getAutoThresholding() {
        return autoThresholding;
    }

    @JIPipeParameter(value = "clahe-enhancing", uiExcludeSubParameters = {"jipipe:data-batch-generation", "jipipe:parameter-slot-algorithm"})
    @JIPipeDocumentation(name = "CLAHE Enhancer", description = "Parameters for underlying CLAHE Enhancing algorithm")
    public CLAHEContrastEnhancer getContrastEnhancer() {
        return contrastEnhancer;
    }

    @JIPipeDocumentation(name = "Apply first CLAHE")
    @JIPipeParameter("apply-first-clahe")
    public boolean isApplyFirstCLAHE() {
        return applyFirstCLAHE;
    }

    @JIPipeParameter("apply-first-clahe")
    public void setApplyFirstCLAHE(boolean applyFirstCLAHE) {
        this.applyFirstCLAHE = applyFirstCLAHE;
    }

    @JIPipeDocumentation(name = "Apply second CLAHE")
    @JIPipeParameter("apply-second-clahe")
    public boolean isApplySecondCLAHE() {
        return applySecondCLAHE;
    }

    @JIPipeParameter("apply-second-clahe")
    public void setApplySecondCLAHE(boolean applySecondCLAHE) {
        this.applySecondCLAHE = applySecondCLAHE;
    }

    @JIPipeDocumentation(name = "Apply Gaussian")
    @JIPipeParameter("apply-gaussian")
    public boolean isApplyGaussian() {
        return applyGaussian;
    }

    @JIPipeParameter("apply-gaussian")
    public void setApplyGaussian(boolean applyGaussian) {
        this.applyGaussian = applyGaussian;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Auto Threshold Segmenter").report(autoThresholding);
        report.forCategory("CLAHE Enhancer").report(contrastEnhancer);
    }
}
