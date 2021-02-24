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

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Segmenter node that thresholds via an auto threshold
 */
@JIPipeDocumentation(name = "Auto threshold 2D", description = "Applies an auto-thresholding algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class AutoThreshold2DAlgorithm extends JIPipeIteratingAlgorithm {

    private AutoThresholder.Method method = AutoThresholder.Method.Default;
    private boolean darkBackground = true;
    private OptionalAnnotationNameParameter thresholdAnnotation = new OptionalAnnotationNameParameter("Threshold", true);
    private SliceThresholdMode thresholdMode = SliceThresholdMode.ApplyPerSlice;

    private DefaultExpressionParameter thresholdCombinationExpression = new DefaultExpressionParameter("MIN(thresholds)");
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;

    /**
     * @param info the info
     */
    public AutoThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        updateRoiSlot();
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
        this.thresholdAnnotation = new OptionalAnnotationNameParameter(other.thresholdAnnotation);
        this.thresholdMode = other.thresholdMode;
        this.thresholdCombinationExpression = new DefaultExpressionParameter(other.thresholdCombinationExpression);
        this.sourceArea = other.sourceArea;
        updateRoiSlot();
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ROIListData roiInput = null;
        ImagePlus maskInput = null;
        AutoThresholder autoThresholder = new AutoThresholder();

        switch (sourceArea) {
            case InsideRoi:
            case OutsideRoi:
                roiInput = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                break;
            case InsideMask:
            case OutsideMask:
                maskInput = dataBatch.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
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
                        index,
                        progressInfo);
                if (!darkBackground)
                    ip.invert();
                int[] histogram = getHistogram(ip, mask);
                int threshold = autoThresholder.getThreshold(method, histogram);
                ip.threshold(threshold);
                thresholds.add(threshold);
            }, progressInfo);
            List<JIPipeAnnotation> annotations = new ArrayList<>();
            if (thresholdAnnotation.isEnabled()) {
                ExpressionParameters variableSet = new ExpressionParameters();
                variableSet.set("thresholds", thresholds);
                String result = thresholdCombinationExpression.evaluate(variableSet) + "";
                annotations.add(thresholdAnnotation.createAnnotation(result));
            }
            dataBatch.addOutputData(getFirstOutputSlot(),
                    new ImagePlusGreyscaleMaskData(img),
                    annotations,
                    JIPipeAnnotationMergeStrategy.Merge,
                    progressInfo);
        } else if (thresholdMode == SliceThresholdMode.CombineSliceHistograms) {
            int[] combinedHistogram = new int[256];
            ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                ImageProcessor mask = getMask(img.getWidth(),
                        img.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index,
                        progressInfo);
                if (!darkBackground)
                    ip.invert();
                int[] histogram = getHistogram(ip, mask);
                for (int i = 0; i < histogram.length; i++) {
                    combinedHistogram[i] += histogram[i];
                }
            }, progressInfo.resolve("Finding histograms"));
            int threshold = autoThresholder.getThreshold(method, combinedHistogram);
            List<JIPipeAnnotation> annotations = new ArrayList<>();
            if (thresholdAnnotation.isEnabled()) {
                annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
            }
            ImageJUtils.forEachSlice(img, ip -> {
                ip.threshold(threshold);
            }, progressInfo);
            dataBatch.addOutputData(getFirstOutputSlot(),
                    new ImagePlusGreyscaleMaskData(img),
                    annotations,
                    JIPipeAnnotationMergeStrategy.Merge,
                    progressInfo);
        } else if (thresholdMode == SliceThresholdMode.CombineThresholdPerSlice) {
            List<Integer> thresholds = new ArrayList<>();
            ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                ImageProcessor mask = getMask(img.getWidth(),
                        img.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index,
                        progressInfo);
                if (!darkBackground)
                    ip.invert();
                int[] histogram = getHistogram(ip, mask);
                int threshold = autoThresholder.getThreshold(method, histogram);
                thresholds.add(threshold);
            }, progressInfo.resolve("Finding thresholds"));

            // Combine thresholds
            ExpressionParameters variableSet = new ExpressionParameters();
            variableSet.set("thresholds", thresholds);
            Number combined = (Number) thresholdCombinationExpression.evaluate(variableSet);
            int threshold = Math.min(255, Math.max(0, combined.intValue()));
            List<JIPipeAnnotation> annotations = new ArrayList<>();
            if (thresholdAnnotation.isEnabled()) {
                annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
            }
            ImageJUtils.forEachSlice(img, ip -> {
                ip.threshold(threshold);
            }, progressInfo);
            dataBatch.addOutputData(getFirstOutputSlot(),
                    new ImagePlusGreyscaleMaskData(img),
                    annotations,
                    JIPipeAnnotationMergeStrategy.OverwriteExisting,
                    progressInfo);
        }
    }

    @JIPipeParameter("method")
    @JIPipeDocumentation(name = "Method")
    public AutoThresholder.Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(AutoThresholder.Method method) {
        this.method = method;
    }

    @JIPipeDocumentation(name = "Dark background", description = "If the background color is dark. Disable this if your image has a bright background.")
    @JIPipeParameter("dark-background")
    public boolean isDarkBackground() {
        return darkBackground;
    }

    @JIPipeParameter("dark-background")
    public void setDarkBackground(boolean darkBackground) {
        this.darkBackground = darkBackground;
    }

    @JIPipeDocumentation(name = "Threshold annotation", description = "Puts the generated threshold(s) into an annotation.")
    @JIPipeParameter("threshold-annotation")
    public OptionalAnnotationNameParameter getThresholdAnnotation() {
        return thresholdAnnotation;
    }

    @JIPipeParameter("threshold-annotation")
    public void setThresholdAnnotation(OptionalAnnotationNameParameter thresholdAnnotation) {
        this.thresholdAnnotation = thresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Multi-slice thresholding", description = "Determines how thresholds are calculated if an image has multiple slices. " +
            "<ul>" +
            "<li><b>Apply threshold per slice</b> calculates and applies the threshold for each slice.</li>" +
            "<li><b>Combine slice histograms</b> merges the slice histograms into one, which is then used for threshold calculation.</li>" +
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

    @JIPipeDocumentation(name = "Threshold combination function", description = "This expression combines multiple thresholds into one numeric threshold.")
    @ExpressionParameterSettings(variableSource = ThresholdsExpressionParameterVariableSource.class)
    @JIPipeParameter("threshold-combine-expression")
    public DefaultExpressionParameter getThresholdCombinationExpression() {
        return thresholdCombinationExpression;
    }

    @JIPipeParameter("threshold-combine-expression")
    public void setThresholdCombinationExpression(DefaultExpressionParameter thresholdCombinationExpression) {
        this.thresholdCombinationExpression = thresholdCombinationExpression;
    }

    @JIPipeDocumentation(name = "Calculate threshold based on ...", description = "Determines from which image areas the pixel values used for calculating the " +
            "thresholds are extracted from.")
    @JIPipeParameter("source-area")
    public ImageROITargetArea getSourceArea() {
        return sourceArea;
    }

    @JIPipeParameter("source-area")
    public void setSourceArea(ImageROITargetArea sourceArea) {
        this.sourceArea = sourceArea;
        updateRoiSlot();
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

    private ImageProcessor getMask(int width, int height, ROIListData rois, ImagePlus mask, ImageSliceIndex sliceIndex, JIPipeProgressInfo progressInfo) {
        switch (sourceArea) {
            case WholeImage: {
                return null;
            }
            case InsideRoi: {
                if (rois.isEmpty()) {
                    return null;
                } else {
                    return rois.getMaskForSlice(width, height,
                            false, true, 0, sliceIndex).getProcessor();
                }
            }
            case OutsideRoi: {
                if (rois.isEmpty()) {
                    return null;
                } else {
                    ImageProcessor processor = rois.getMaskForSlice(width, height,
                            false, true, 0, sliceIndex).getProcessor();
                    processor.invert();
                    return processor;
                }
            }
            case InsideMask: {
                if (mask.getStackSize() > 1) {
                    return mask.getStack().getProcessor(sliceIndex.getStackIndex(mask));
                } else {
                    return mask.getProcessor();
                }
            }
            case OutsideMask: {
                ImageProcessor processor;
                if (mask.getStackSize() > 1) {
                    processor = mask.getStack().getProcessor(sliceIndex.getStackIndex(mask)).duplicate();
                } else {
                    processor = mask.getProcessor().duplicate();
                }
                processor.invert();
                return processor;
            }
        }
        throw new UnsupportedOperationException();
    }

    private void updateRoiSlot() {
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        if (sourceArea == ImageROITargetArea.WholeImage) {
            if (slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if (slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        } else if (sourceArea == ImageROITargetArea.InsideRoi || sourceArea == ImageROITargetArea.OutsideRoi) {
            if (!slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.addSlot("ROI", new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, "ROI"), false);
            }
            if (slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        } else if (sourceArea == ImageROITargetArea.InsideMask || sourceArea == ImageROITargetArea.OutsideMask) {
            if (slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if (!slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.addSlot("Mask", new JIPipeDataSlotInfo(ImagePlusGreyscaleMaskData.class, JIPipeSlotType.Input, "Mask"), false);
            }
        }
    }

    public enum SliceThresholdMode {
        ApplyPerSlice,
        CombineSliceHistograms,
        CombineThresholdPerSlice;


        @Override
        public String toString() {
            switch (this) {
                case ApplyPerSlice:
                    return "Apply threshold per slice";
                case CombineSliceHistograms:
                    return "Combine slice histograms";
                case CombineThresholdPerSlice:
                    return "Combine thresholds per slice";
            }
            throw new UnsupportedOperationException();
        }
    }
}
