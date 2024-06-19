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

import ij.ImagePlus;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;

import java.util.ArrayList;
import java.util.List;


/**
 * Thresholding node that thresholds via an auto threshold
 */
@SetJIPipeDocumentation(name = "Auto threshold 2D", description = "Applies an auto-thresholding algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust", aliasName = "Auto Threshold")
public class AutoThreshold2DAlgorithm extends JIPipeIteratingAlgorithm {

    private AutoThresholder.Method method = AutoThresholder.Method.Default;
    private boolean darkBackground = true;
    private OptionalTextAnnotationNameParameter thresholdAnnotation = new OptionalTextAnnotationNameParameter("Threshold", true);
    private SliceThresholdMode thresholdMode = SliceThresholdMode.ApplyPerSlice;

    private JIPipeExpressionParameter thresholdCombinationExpression = new JIPipeExpressionParameter("MIN(thresholds)");
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * @param info the info
     */
    public AutoThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AutoThreshold2DAlgorithm(AutoThreshold2DAlgorithm other) {
        super(other);
        this.method = other.method;
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
            ImagePlusData inputData = iterationStep.getInputData("Input", ImagePlusGreyscale8UData.class, progressInfo);
            ImagePlus img = inputData.getDuplicateImage();
            ROIListData roiInput = null;
            ImagePlus maskInput = null;
            AutoThresholder autoThresholder = new AutoThresholder();

            switch (sourceArea) {
                case InsideRoi:
                case OutsideRoi:
                    roiInput = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
                    break;
                case InsideMask:
                case OutsideMask:
                    maskInput = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
                    break;
            }

            ROIListData finalRoiInput = roiInput;
            ImagePlus finalMaskInput = maskInput;

            if (thresholdMode == SliceThresholdMode.ApplyPerSlice) {
                List<Integer> thresholds = new ArrayList<>();
                ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    ImageProcessor mask = getMask(img.getWidth(),
                            img.getHeight(),
                            finalRoiInput,
                            finalMaskInput,
                            index
                    );
                    if (!darkBackground)
                        ip.invert();
                    int[] histogram = getHistogram(ip, mask);
                    int threshold = autoThresholder.getThreshold(method, histogram);
                    ip.threshold(threshold);
                    thresholds.add(threshold);
                }, progressInfo);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (thresholdAnnotation.isEnabled()) {
                    JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
                    variableSet.set("thresholds", thresholds);
                    String result = thresholdCombinationExpression.evaluate(variableSet) + "";
                    annotations.add(thresholdAnnotation.createAnnotation(result));
                }
                iterationStep.addOutputData(getFirstOutputSlot(),
                        new ImagePlusGreyscaleMaskData(img),
                        annotations,
                        JIPipeTextAnnotationMergeMode.Merge,
                        progressInfo);
            } else if (thresholdMode == SliceThresholdMode.CombineSliceStatistics) {
                int[] combinedHistogram = new int[256];
                ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    ImageProcessor mask = getMask(img.getWidth(),
                            img.getHeight(),
                            finalRoiInput,
                            finalMaskInput,
                            index
                    );
                    if (!darkBackground)
                        ip.invert();
                    int[] histogram = getHistogram(ip, mask);
                    for (int i = 0; i < histogram.length; i++) {
                        combinedHistogram[i] += histogram[i];
                    }
                }, progressInfo.resolve("Finding histograms"));
                int threshold = autoThresholder.getThreshold(method, combinedHistogram);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (thresholdAnnotation.isEnabled()) {
                    annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
                }
                ImageJUtils.forEachSlice(img, ip -> {
                    ip.threshold(threshold);
                }, progressInfo);
                iterationStep.addOutputData(getFirstOutputSlot(),
                        new ImagePlusGreyscaleMaskData(img),
                        annotations,
                        JIPipeTextAnnotationMergeMode.Merge,
                        progressInfo);
            } else if (thresholdMode == SliceThresholdMode.CombineThresholdPerSlice) {
                List<Integer> thresholds = new ArrayList<>();
                ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    ImageProcessor mask = getMask(img.getWidth(),
                            img.getHeight(),
                            finalRoiInput,
                            finalMaskInput,
                            index
                    );
                    if (!darkBackground)
                        ip.invert();
                    int[] histogram = getHistogram(ip, mask);
                    int threshold = autoThresholder.getThreshold(method, histogram);
                    thresholds.add(threshold);
                }, progressInfo.resolve("Finding thresholds"));

                // Combine thresholds
                JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
                variableSet.set("thresholds", thresholds);
                Number combined = (Number) thresholdCombinationExpression.evaluate(variableSet);
                int threshold = Math.min(255, Math.max(0, combined.intValue()));
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (thresholdAnnotation.isEnabled()) {
                    annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
                }
                ImageJUtils.forEachSlice(img, ip -> {
                    ip.threshold(threshold);
                }, progressInfo);
                iterationStep.addOutputData(getFirstOutputSlot(),
                        new ImagePlusGreyscaleMaskData(img),
                        annotations,
                        thresholdAnnotationStrategy,
                        progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Threshold annotation strategy", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeTextAnnotationMergeMode getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }

    @JIPipeParameter(value = "method", important = true)
    @SetJIPipeDocumentation(name = "Method")
    public AutoThresholder.Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(AutoThresholder.Method method) {
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
    public SliceThresholdMode getThresholdMode() {
        return thresholdMode;
    }

    @JIPipeParameter("slice-threshold-mode")
    public void setThresholdMode(SliceThresholdMode thresholdMode) {
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

    private int[] getHistogram(ImageProcessor ip, ImageProcessor foregroundMask) {
        if (foregroundMask == null) {
            return ip.getHistogram();
        } else {
            // Mask the original processor
            ImageProcessor backgroundMask = foregroundMask.duplicate();
            backgroundMask.invert();
            ImageProcessor maskedIp = ip.duplicate();
            maskedIp.setMask(backgroundMask);
            maskedIp.setValue(0);
            maskedIp.fill(backgroundMask);
            maskedIp.resetRoi();

            // Get the histogram
            int[] histogram = maskedIp.getHistogram();

            // Count how many black pixels the mask has
            int[] maskHistogram = foregroundMask.getHistogram();

            // Remove the number of black pixels from the histogram
            histogram[0] -= maskHistogram[0];

            return histogram;
        }
    }

    private ImageProcessor getMask(int width, int height, ROIListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
        return ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea, width, height, rois, mask, sliceIndex);
    }

    public enum SliceThresholdMode {
        ApplyPerSlice,
        CombineSliceStatistics,
        CombineThresholdPerSlice;


        @Override
        public String toString() {
            switch (this) {
                case ApplyPerSlice:
                    return "Apply threshold per slice";
                case CombineSliceStatistics:
                    return "Combine slice statistics";
                case CombineThresholdPerSlice:
                    return "Combine thresholds per slice";
            }
            throw new UnsupportedOperationException();
        }
    }
}
