/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;
import org.hkijena.jipipe.utils.threshold.NBinsAutoThresholder;

import java.util.ArrayList;
import java.util.List;

/**
 * Thresholding node that thresholds via an auto threshold applied to a histogram with a custom number of bins
 */
@SetJIPipeDocumentation(name = "Auto threshold 2D (custom bins)", description = "Applies an auto-thresholding algorithm to a histogram " +
        "with a custom number of bins. 8-bit and 16-bit inputs are binned over their full native range. " +
        "For 32-bit inputs, the bin range is determined by the multi-slice thresholding mode: " +
        "'Apply threshold per slice' uses each slice's minimum/maximum, 'Combine slice statistics' uses " +
        "the whole image's minimum/maximum, and 'Combine thresholds per slice' computes per-slice thresholds " +
        "that are combined via the expression. The threshold is mapped back to a pixel value via the bin range. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
public class AutoThreshold2DNBinsAlgorithm extends JIPipeIteratingAlgorithm {

    public static int binIndex(double value, double min, double max, int nbins) {
        if (max <= min)
            return 0;
        int index = (int) Math.floor((value - min) / (max - min) * nbins);
        return Math.min(nbins - 1, Math.max(0, index));
    }

    private AutoThresholdMethod method = AutoThresholdMethod.Default;
    private int nbins = 256;
    private boolean darkBackground = true;
    private OptionalTextAnnotationNameParameter thresholdAnnotation = new OptionalTextAnnotationNameParameter("Threshold", false);
    private AutoThreshold2DAlgorithm.SliceThresholdMode thresholdMode = AutoThreshold2DAlgorithm.SliceThresholdMode.ApplyPerSlice;
    private JIPipeExpressionParameter thresholdCombinationExpression = new JIPipeExpressionParameter("MIN(thresholds)");
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * @param info the info
     */
    public AutoThreshold2DNBinsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AutoThreshold2DNBinsAlgorithm(AutoThreshold2DNBinsAlgorithm other) {
        super(other);
        this.method = other.method;
        this.nbins = other.nbins;
        this.darkBackground = other.darkBackground;
        this.thresholdAnnotation = new OptionalTextAnnotationNameParameter(other.thresholdAnnotation);
        this.thresholdMode = other.thresholdMode;
        this.thresholdCombinationExpression = new JIPipeExpressionParameter(other.thresholdCombinationExpression);
        this.sourceArea = other.sourceArea;
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            ImagePlusData inputData = iterationStep.getInputData("Input", ImagePlusGreyscaleData.class, progressInfo);
            ImagePlus img = inputData.getDuplicateImage();
            Roi2dListData roiInput = null;
            ImagePlus maskInput = null;

            switch (sourceArea) {
                case InsideRoi:
                case OutsideRoi:
                    roiInput = iterationStep.getInputData("ROI", Roi2dListData.class, progressInfo);
                    break;
                case InsideMask:
                case OutsideMask:
                    maskInput = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
                    break;
            }

            Roi2dListData finalRoiInput = roiInput;
            ImagePlus finalMaskInput = maskInput;

            ImagePlus outputImage = IJ.createHyperStack(img.getTitle() + " Thresholded",
                    img.getWidth(),
                    img.getHeight(),
                    img.getNChannels(),
                    img.getNSlices(),
                    img.getNFrames(),
                    8);

            if (thresholdMode == AutoThreshold2DAlgorithm.SliceThresholdMode.ApplyPerSlice) {
                List<Double> thresholds = new ArrayList<>();
                ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    ImageProcessor mask = getMask(img.getWidth(),
                            img.getHeight(),
                            finalRoiInput,
                            finalMaskInput,
                            index
                    );
                    double[] range = getBinRange(ip);
                    double min = range[0];
                    double max = range[1];
                    if (!darkBackground)
                        invert(ip, min, max);
                    int[] histogram = getHistogram(ip, mask, min, max);
                    int threshold = NBinsAutoThresholder.getThreshold(method, histogram);
                    double thresholdValue = mapToValue(threshold, min, max);
                    ByteProcessor targetProcessor = getTargetProcessor(outputImage, index);
                    applyThreshold(ip, targetProcessor, thresholdValue);
                    thresholds.add(thresholdValue);
                }, progressInfo);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (thresholdAnnotation.isEnabled()) {
                    JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);
                    variableSet.set("thresholds", thresholds);
                    String result = thresholdCombinationExpression.evaluate(variableSet) + "";
                    annotations.add(thresholdAnnotation.createAnnotation(result));
                }
                iterationStep.addOutputData(getFirstOutputSlot(),
                        new ImagePlusGreyscaleMaskData(outputImage),
                        annotations,
                        JIPipeTextAnnotationMergeMode.Merge,
                        progressInfo);
            } else if (thresholdMode == AutoThreshold2DAlgorithm.SliceThresholdMode.CombineSliceStatistics) {
                // Determine the bin range. For 32-bit inputs this is the whole image's min/max; for integer types it is the full native range.
                double globalMin;
                double globalMax;
                if (img.getProcessor() instanceof FloatProcessor) {
                    double[] range = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
                    ImageJIterationUtils.forEachSlice(img, ip -> {
                        ip.resetMinAndMax();
                        range[0] = Math.min(range[0], ip.getMin());
                        range[1] = Math.max(range[1], ip.getMax());
                    }, progressInfo.resolve("Finding min/max"));
                    globalMin = range[0];
                    globalMax = range[1];
                } else if (img.getProcessor() instanceof ShortProcessor) {
                    globalMin = 0;
                    globalMax = 65535;
                } else {
                    globalMin = 0;
                    globalMax = 255;
                }
                final double min = globalMin;
                final double max = globalMax;
                int[] combinedHistogram = new int[nbins];
                ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    ImageProcessor mask = getMask(img.getWidth(),
                            img.getHeight(),
                            finalRoiInput,
                            finalMaskInput,
                            index
                    );
                    if (!darkBackground)
                        invert(ip, min, max);
                    int[] histogram = getHistogram(ip, mask, min, max);
                    for (int i = 0; i < histogram.length; i++) {
                        combinedHistogram[i] += histogram[i];
                    }
                }, progressInfo.resolve("Finding histograms"));
                int threshold = NBinsAutoThresholder.getThreshold(method, combinedHistogram);
                double thresholdValue = mapToValue(threshold, min, max);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (thresholdAnnotation.isEnabled()) {
                    annotations.add(thresholdAnnotation.createAnnotation("" + thresholdValue));
                }
                ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    ByteProcessor targetProcessor = getTargetProcessor(outputImage, index);
                    applyThreshold(ip, targetProcessor, thresholdValue);
                }, progressInfo);
                iterationStep.addOutputData(getFirstOutputSlot(),
                        new ImagePlusGreyscaleMaskData(outputImage),
                        annotations,
                        JIPipeTextAnnotationMergeMode.Merge,
                        progressInfo);
            } else if (thresholdMode == AutoThreshold2DAlgorithm.SliceThresholdMode.CombineThresholdPerSlice) {
                List<Double> thresholds = new ArrayList<>();
                ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    ImageProcessor mask = getMask(img.getWidth(),
                            img.getHeight(),
                            finalRoiInput,
                            finalMaskInput,
                            index
                    );
                    double[] range = getBinRange(ip);
                    double min = range[0];
                    double max = range[1];
                    if (!darkBackground)
                        invert(ip, min, max);
                    int[] histogram = getHistogram(ip, mask, min, max);
                    int threshold = NBinsAutoThresholder.getThreshold(method, histogram);
                    thresholds.add(mapToValue(threshold, min, max));
                }, progressInfo.resolve("Finding thresholds"));

                // Combine thresholds
                JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);
                variableSet.set("thresholds", thresholds);
                Number combined = (Number) thresholdCombinationExpression.evaluate(variableSet);
                double thresholdValue = combined.doubleValue();
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (thresholdAnnotation.isEnabled()) {
                    annotations.add(thresholdAnnotation.createAnnotation("" + thresholdValue));
                }
                ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    double[] range = getBinRange(ip);
                    double clampedThresholdValue = Math.min(range[1], Math.max(range[0], thresholdValue));
                    ByteProcessor targetProcessor = getTargetProcessor(outputImage, index);
                    applyThreshold(ip, targetProcessor, clampedThresholdValue);
                }, progressInfo);
                iterationStep.addOutputData(getFirstOutputSlot(),
                        new ImagePlusGreyscaleMaskData(outputImage),
                        annotations,
                        thresholdAnnotationStrategy,
                        progressInfo);
            }
        }
    }

    private void applyThreshold(ImageProcessor source, ByteProcessor target, double threshold) {
        int size = source.getWidth() * source.getHeight();
        byte[] dst = (byte[]) target.getPixels();
        for (int i = 0; i < size; i++) {
            dst[i] = source.getf(i) > threshold ? (byte) 255 : 0;
        }
    }

    /**
     * Returns the bin range for the given processor:
     * 8-bit and 16-bit inputs are binned over their full native range; for 32-bit inputs the actual data
     * minimum/maximum of the slice is used.
     *
     * @param ip the processor
     * @return {min, max}
     */
    private double[] getBinRange(ImageProcessor ip) {
        if (ip instanceof FloatProcessor) {
            // getMin()/getMax() can return a fixed display range if setMinAndMax() was called before;
            // reset first to guarantee the actual data range
            ip.resetMinAndMax();
            return new double[]{ip.getMin(), ip.getMax()};
        } else if (ip instanceof ShortProcessor) {
            return new double[]{0, 65535};
        } else {
            return new double[]{0, 255};
        }
    }

    /**
     * Maps a threshold bin index back to a pixel value.
     * A pixel is foreground if its value is larger than the returned threshold.
     *
     * @param t   the threshold bin index
     * @param min the minimum of the bin range
     * @param max the maximum of the bin range
     * @return the threshold as pixel value
     */
    private double mapToValue(int t, double min, double max) {
        return min + (t + 1) * (max - min) / nbins;
    }

    /**
     * Inverts a slice over the given range (v -> (min + max) - v).
     * <p>
     * This is applied when the background is bright, so that foreground pixels become bright.
     * {@link ShortProcessor#invert()} cannot be used for 16-bit inputs: unless the (off by default)
     * {@code Prefs.fullRange16bitInversions} preference is set, it inverts around the
     * current min/max display range (v -> max + min - v of the slice's data range).
     *
     * @param ip  the processor to invert in-place
     * @param min the minimum of the range used for binning
     * @param max the maximum of the range used for binning
     */
    private static void invert(ImageProcessor ip, double min, double max) {
        if (ip instanceof ShortProcessor) {
            short[] pixels = (short[]) ip.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = (short) (65535 - (pixels[i] & 0xffff));
            }
        } else if (ip instanceof FloatProcessor) {
            float[] pixels = (float[]) ip.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = (float) ((min + max) - pixels[i]);
            }
        } else {
            ip.invert();
        }
    }

    private int[] getHistogram(ImageProcessor ip, ImageProcessor foregroundMask, double min, double max) {
        int[] histogram = new int[nbins];
        if (foregroundMask == null) {
            int size = ip.getWidth() * ip.getHeight();
            for (int i = 0; i < size; i++) {
                histogram[binIndex(ip.getf(i), min, max, nbins)]++;
            }
            return histogram;
        } else {
            int size = ip.getWidth() * ip.getHeight();
            byte[] maskPixels = (byte[]) foregroundMask.getPixels();
            for (int i = 0; i < size; i++) {
                if (Byte.toUnsignedInt(maskPixels[i]) > 0) {
                    histogram[binIndex(ip.getf(i), min, max, nbins)]++;
                }
            }
            return histogram;
        }
    }

    private ByteProcessor getTargetProcessor(ImagePlus outputImage, ImageSliceIndex index) {
        return (ByteProcessor) (outputImage.hasImageStack() ?
                outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                : outputImage.getProcessor());
    }

    private ImageProcessor getMask(int width, int height, Roi2dListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
        return ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea, width, height, rois, mask, sliceIndex);
    }

    @SetJIPipeDocumentation(name = "Number of bins", description = "The number of histogram bins used to calculate the threshold. " +
            "The minimum is 2 and the maximum is 65536.")
    @JIPipeParameter(value = "nbins", important = true)
    public int getNbins() {
        return nbins;
    }

    @JIPipeParameter("nbins")
    public boolean setNbins(int nbins) {
        if (nbins < 2 || nbins > 65536)
            return false;
        this.nbins = nbins;
        return true;
    }

    @SetJIPipeDocumentation(name = "Threshold annotation mode", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeTextAnnotationMergeMode getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }

    @SetJIPipeDocumentation(name = "Method")
    @JIPipeParameter(value = "method", important = true)
    public AutoThresholdMethod getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(AutoThresholdMethod method) {
        this.method = method;
    }

    @SetJIPipeDocumentation(name = "Dark background", description = "If the background color is dark. Disable this if your image has a bright background.")
    @JIPipeParameter("dark-background")
    public boolean isDarkBackground() {
        return darkBackground;
    }

    @JIPipeParameter("dark-background")
    public void setDarkBackground(boolean darkBackground) {
        this.darkBackground = darkBackground;
    }

    @SetJIPipeDocumentation(name = "Threshold annotation", description = "Puts the generated threshold(s) into an annotation.")
    @JIPipeParameter("threshold-annotation")
    public OptionalTextAnnotationNameParameter getThresholdAnnotation() {
        return thresholdAnnotation;
    }

    @JIPipeParameter("threshold-annotation")
    public void setThresholdAnnotation(OptionalTextAnnotationNameParameter thresholdAnnotation) {
        this.thresholdAnnotation = thresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Multi-slice thresholding", description = "Determines how thresholds are calculated if an image has multiple slices. " +
            "<ul>" +
            "<li><b>Apply threshold per slice</b> calculates and applies the threshold for each slice.</li>" +
            "<li><b>Combine slice statistics</b> merges the slice histograms into one, which is then used for threshold calculation.</li>" +
            "<li><b>Combine thresholds per slice</b> calculates the threshold for each slice. One threshold for all slices is calculated via the math expression.</li>" +
            "</ul>")
    @JIPipeParameter("slice-threshold-mode")
    public AutoThreshold2DAlgorithm.SliceThresholdMode getThresholdMode() {
        return thresholdMode;
    }

    @JIPipeParameter("slice-threshold-mode")
    public void setThresholdMode(AutoThreshold2DAlgorithm.SliceThresholdMode thresholdMode) {
        this.thresholdMode = thresholdMode;
    }

    @SetJIPipeDocumentation(name = "Threshold combination function", description = "This expression combines multiple thresholds into one numeric threshold.")
    @JIPipeExpressionParameterSettings(variableSource = ThresholdsExpressionParameterVariablesInfo.class)
    @JIPipeParameter("threshold-combine-expression")
    public JIPipeExpressionParameter getThresholdCombinationExpression() {
        return thresholdCombinationExpression;
    }

    @JIPipeParameter("threshold-combine-expression")
    public void setThresholdCombinationExpression(JIPipeExpressionParameter thresholdCombinationExpression) {
        this.thresholdCombinationExpression = thresholdCombinationExpression;
    }

    @SetJIPipeDocumentation(name = "Calculate threshold based on ...", description = "Determines from which image areas the pixel values used for calculating the " +
            "thresholds are extracted from.")
    @JIPipeParameter("source-area")
    public ImageROITargetArea getSourceArea() {
        return sourceArea;
    }

    @JIPipeParameter("source-area")
    public void setSourceArea(ImageROITargetArea sourceArea) {
        this.sourceArea = sourceArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }
}
