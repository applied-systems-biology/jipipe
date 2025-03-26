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

import com.google.common.primitives.Longs;
import gnu.trove.list.array.TByteArrayList;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Thresholding node that thresholds via an auto threshold
 */
@SetJIPipeDocumentation(name = "Custom auto threshold 2D (8-bit)", description = "Allows to implement a custom thresholding method via expressions. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust", aliasName = "Auto threshold (custom, 8-bit)")
public class CustomAutoThreshold2D8UAlgorithm extends JIPipeIteratingAlgorithm {

    private JIPipeExpressionParameter thresholdCalculationExpression = new JIPipeExpressionParameter("(stat_max + stat_min) / 2");
    private OptionalTextAnnotationNameParameter thresholdAnnotation = new OptionalTextAnnotationNameParameter("Threshold", false);
    private AutoThreshold2DAlgorithm.SliceThresholdMode thresholdMode = AutoThreshold2DAlgorithm.SliceThresholdMode.ApplyPerSlice;

    private JIPipeExpressionParameter thresholdCombinationExpression = new JIPipeExpressionParameter("MIN(thresholds)");
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    private boolean accessPixels = true;

    /**
     * @param info the info
     */
    public CustomAutoThreshold2D8UAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public CustomAutoThreshold2D8UAlgorithm(CustomAutoThreshold2D8UAlgorithm other) {
        super(other);
        this.thresholdCalculationExpression = new JIPipeExpressionParameter(other.thresholdCalculationExpression);
        this.thresholdAnnotation = new OptionalTextAnnotationNameParameter(other.thresholdAnnotation);
        this.thresholdMode = other.thresholdMode;
        this.thresholdCombinationExpression = new JIPipeExpressionParameter(other.thresholdCombinationExpression);
        this.sourceArea = other.sourceArea;
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
        this.accessPixels = other.accessPixels;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ROI2DListData roiInput = null;
        ImagePlus maskInput = null;
        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap(iterationStep);

        parameters.set("width", img.getWidth());
        parameters.set("height", img.getHeight());
        parameters.set("size_z", img.getNSlices());
        parameters.set("size_c", img.getNChannels());
        parameters.set("size_t", img.getNFrames());

        switch (sourceArea) {
            case InsideRoi:
            case OutsideRoi:
                roiInput = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
                break;
            case InsideMask:
            case OutsideMask:
                maskInput = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
                break;
        }

        ROI2DListData finalRoiInput = roiInput;
        ImagePlus finalMaskInput = maskInput;

        if (thresholdMode == AutoThreshold2DAlgorithm.SliceThresholdMode.ApplyPerSlice) {
            List<Integer> thresholds = new ArrayList<>();
            TByteArrayList pixels = new TByteArrayList(img.getWidth() * img.getHeight());
            ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                ImageProcessor mask = ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea,
                        img.getWidth(),
                        img.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index
                );

                // Collect all pixels
                pixels.clear();
                ImageJUtils.getMaskedPixels_8U(ip, mask, pixels);
                ImageStatistics statistics = new ByteProcessor(pixels.size(), 1, pixels.toArray()).getStatistics();
                int threshold = getThreshold(parameters, statistics, pixels);
                ip.threshold(threshold);
                thresholds.add(threshold);
            }, progressInfo);
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (thresholdAnnotation.isEnabled()) {
                JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);
                variableSet.set("thresholds", thresholds);
                String result = thresholdCombinationExpression.evaluate(variableSet) + "";
                annotations.add(thresholdAnnotation.createAnnotation(result));
            }
            iterationStep.addOutputData(getFirstOutputSlot(),
                    new ImagePlusGreyscaleMaskData(img),
                    annotations,
                    JIPipeTextAnnotationMergeMode.Merge,
                    progressInfo);
        } else if (thresholdMode == AutoThreshold2DAlgorithm.SliceThresholdMode.CombineSliceStatistics) {
            TByteArrayList pixels = new TByteArrayList(img.getWidth() * img.getHeight() *
                    img.getNFrames() * img.getNChannels() * img.getNSlices());
            ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                ImageProcessor mask = ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea,
                        img.getWidth(),
                        img.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index
                );
                ImageJUtils.getMaskedPixels_8U(ip, mask, pixels);
            }, progressInfo.resolve("Combining pixels"));
            ImageStatistics statistics = new ByteProcessor(pixels.size(), 1, pixels.toArray()).getStatistics();
            int threshold = getThreshold(parameters, statistics, pixels);
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (thresholdAnnotation.isEnabled()) {
                annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
            }
            ImageJIterationUtils.forEachSlice(img, ip -> {
                ip.threshold(threshold);
            }, progressInfo);
            iterationStep.addOutputData(getFirstOutputSlot(),
                    new ImagePlusGreyscaleMaskData(img),
                    annotations,
                    JIPipeTextAnnotationMergeMode.Merge,
                    progressInfo);
        } else if (thresholdMode == AutoThreshold2DAlgorithm.SliceThresholdMode.CombineThresholdPerSlice) {
            List<Integer> thresholds = new ArrayList<>();
            TByteArrayList pixels = new TByteArrayList(img.getWidth() * img.getHeight());
            ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                ImageProcessor mask = ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea,
                        img.getWidth(),
                        img.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index
                );
                pixels.clear();
                ImageJUtils.getMaskedPixels_8U(ip, mask, pixels);
                ImageStatistics statistics = new ByteProcessor(pixels.size(), 1, pixels.toArray()).getStatistics();
                int threshold = getThreshold(parameters, statistics, pixels);
                thresholds.add(threshold);
            }, progressInfo.resolve("Finding thresholds"));

            // Combine thresholds
            JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);
            variableSet.set("thresholds", thresholds);
            Number combined = (Number) thresholdCombinationExpression.evaluate(variableSet);
            int threshold = combined.intValue();
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (thresholdAnnotation.isEnabled()) {
                annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
            }
            ImageJIterationUtils.forEachSlice(img, ip -> {
                ip.threshold(threshold);
            }, progressInfo);
            iterationStep.addOutputData(getFirstOutputSlot(),
                    new ImagePlusGreyscaleMaskData(img),
                    annotations,
                    thresholdAnnotationStrategy,
                    progressInfo);
        }
    }

    private int getThreshold(JIPipeExpressionVariablesMap parameters, ImageStatistics statistics, TByteArrayList pixels) {
        parameters.set("stat_histogram", Longs.asList(statistics.getHistogram()));
        parameters.set("stat_area", statistics.area);
        parameters.set("stat_stdev", statistics.stdDev);
        parameters.set("stat_min", statistics.min);
        parameters.set("stat_max", statistics.max);
        parameters.set("stat_mean", statistics.mean);
        parameters.set("stat_mode", statistics.dmode);
        parameters.set("stat_median", statistics.median);
        parameters.set("stat_kurtosis", statistics.kurtosis);
        parameters.set("stat_int_den", statistics.area * statistics.mean);
        parameters.set("stat_raw_int_den", statistics.pixelCount * statistics.umean);
        parameters.set("stat_skewness", statistics.skewness);
        parameters.set("stat_area_fraction", statistics.areaFraction);
        if (accessPixels) {
            List<Integer> pixelList = new ArrayList<>();
            for (int i = 0; i < pixels.size(); i++) {
                pixelList.add(Byte.toUnsignedInt(pixels.get(i)));
            }
            parameters.set("pixels", pixelList);
        }
        Number number = (Number) thresholdCalculationExpression.evaluate(parameters);
        return number.intValue();
    }

    @SetJIPipeDocumentation(name = "Thresholding function", description = "This expression is executed for each set of slices. " +
            "The expression should return a number that will be used as threshold. A pixel is set to 255 if its value is larger than " +
            "this threshold.")
    @JIPipeParameter(value = "thresholding-function", important = true)
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getThresholdCalculationExpression() {
        return thresholdCalculationExpression;
    }

    @JIPipeParameter("thresholding-function")
    public void setThresholdCalculationExpression(JIPipeExpressionParameter thresholdCalculationExpression) {
        this.thresholdCalculationExpression = thresholdCalculationExpression;
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
            "<li><b>Combine slice statistics</b> calculates statistics for all pixels in the image.</li>" +
            "<li><b>Combine thresholds per slice</b> calculates the threshold for each slice. One threshold for all slices is calculated via another math expression.</li>" +
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

    @SetJIPipeDocumentation(name = "Threshold annotation mode", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeTextAnnotationMergeMode getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }

    @SetJIPipeDocumentation(name = "Pixels are available as variables", description = "If enabled, the list of all pixels that are considered for the statistics are available as variable 'pixels'. " +
            "Please note that this will slow down the performance due to necessary conversions.")
    @JIPipeParameter("access-pixels")
    public boolean isAccessPixels() {
        return accessPixels;
    }

    @JIPipeParameter("access-pixels")
    public void setAccessPixels(boolean accessPixels) {
        this.accessPixels = accessPixels;
    }

    public static class VariablesInfo implements JIPipeExpressionVariablesInfo {
        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
            result.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            result.add(new JIPipeExpressionParameterVariableInfo("width", "Image width", "The width of the image"));
            result.add(new JIPipeExpressionParameterVariableInfo("height", "Image height", "The height of the image"));
            result.add(new JIPipeExpressionParameterVariableInfo("size_z", "Image Z slices", "The number of Z slices in the image"));
            result.add(new JIPipeExpressionParameterVariableInfo("size_c", "Image channels", "The number of channel (C) slices in the image"));
            result.add(new JIPipeExpressionParameterVariableInfo("size_t", "Image frames", "The number of frames (T) in the image"));
            result.add(new JIPipeExpressionParameterVariableInfo("pixels", "Pixels", "Array of numbers that contain the pixel values"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_histogram", "Histogram",
                    "An array the represents the histogram (index is the pixel value, value is the number of pixels with this value) of the currently analyzed area"
            ));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_area", "Area", "Area of selection in square pixels."));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_stdev", "Pixel value standard deviation", "Measures the standard deviation of greyscale pixel values"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_min", "Pixel value min", "Measures the minimum of greyscale pixel values"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_max", "Pixel value max", "Measures the maximum of greyscale pixel values"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_mean", "Pixel value mean", "Measures the mean of greyscale pixel values"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_mode", "Pixel value mode", "Most frequently occurring gray value within the selection"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_median", "Pixel value median", "The median value of the pixels in the image or selection"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_kurtosis", "Pixel value kurtosis", "The fourth order moment about the greyscale pixel value mean"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_int_den", "Pixel value integrated density", "The product of Area and Mean Gray Value"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_raw_int_den", "Pixel value raw integrated density", "The sum of the values of the pixels in the image or selection"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_skewness", "Pixel value skewness", "The sum of the values of the pixels in the image or selection"));
            result.add(new JIPipeExpressionParameterVariableInfo("stat_area_fraction", "Area fraction", "The percentage of non-zero pixels"));
            return result;
        }
    }
}
