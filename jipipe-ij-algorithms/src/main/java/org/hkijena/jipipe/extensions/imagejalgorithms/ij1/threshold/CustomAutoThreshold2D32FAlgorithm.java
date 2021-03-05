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

import com.google.common.primitives.Longs;
import gnu.trove.list.array.TFloatArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.process.*;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.expressions.*;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Thresholding node that thresholds via an auto threshold
 */
@JIPipeDocumentation(name = "Custom auto threshold 2D (32-bit)", description = "Allows to implement a custom thresholding method via expressions. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class CustomAutoThreshold2D32FAlgorithm extends JIPipeIteratingAlgorithm {

    private DefaultExpressionParameter thresholdCalculationExpression = new DefaultExpressionParameter("(max_value + min_value) / 2");
    private OptionalAnnotationNameParameter thresholdAnnotation = new OptionalAnnotationNameParameter("Threshold", true);
    private AutoThreshold2DAlgorithm.SliceThresholdMode thresholdMode = AutoThreshold2DAlgorithm.SliceThresholdMode.ApplyPerSlice;

    private DefaultExpressionParameter thresholdCombinationExpression = new DefaultExpressionParameter("MIN(thresholds)");
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private JIPipeAnnotationMergeStrategy thresholdAnnotationStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

    /**
     * @param info the info
     */
    public CustomAutoThreshold2D32FAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale16UData.class)
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
    public CustomAutoThreshold2D32FAlgorithm(CustomAutoThreshold2D32FAlgorithm other) {
        super(other);
        this.thresholdCalculationExpression = new DefaultExpressionParameter(other.thresholdCalculationExpression);
        this.thresholdAnnotation = new OptionalAnnotationNameParameter(other.thresholdAnnotation);
        this.thresholdMode = other.thresholdMode;
        this.thresholdCombinationExpression = new DefaultExpressionParameter(other.thresholdCombinationExpression);
        this.sourceArea = other.sourceArea;
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
        updateRoiSlot();
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo);
        ImagePlus inputImage = inputData.getImage();
        ImagePlus outputImage = IJ.createHyperStack(inputImage.getTitle() + " Thresholded",
                inputImage.getWidth(),
                inputImage.getHeight(),
                inputImage.getNChannels(),
                inputImage.getNSlices(),
                inputImage.getNFrames(),
                8);
        ROIListData roiInput = null;
        ImagePlus maskInput = null;
        ExpressionParameters parameters = new ExpressionParameters();
        parameters.set("width", inputImage.getWidth());
        parameters.set("height", inputImage.getHeight());
        parameters.set("size_z", inputImage.getNSlices());
        parameters.set("size_c", inputImage.getNChannels());
        parameters.set("size_t", inputImage.getNFrames());

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

        if (thresholdMode == AutoThreshold2DAlgorithm.SliceThresholdMode.ApplyPerSlice) {
            thresholdApplyPerSlice(dataBatch, progressInfo, inputImage, outputImage, parameters, finalRoiInput, finalMaskInput);
        } else if (thresholdMode == AutoThreshold2DAlgorithm.SliceThresholdMode.CombineSliceStatistics) {
            thresholdCombineStatistics(dataBatch, progressInfo, inputImage, outputImage, parameters, finalRoiInput, finalMaskInput);
        } else if (thresholdMode == AutoThreshold2DAlgorithm.SliceThresholdMode.CombineThresholdPerSlice) {
            thresholdCombineThresholds(dataBatch, progressInfo, inputImage, outputImage, parameters, finalRoiInput, finalMaskInput);
        }
    }

    private void thresholdCombineThresholds(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo, ImagePlus inputImage, ImagePlus outputImage, ExpressionParameters parameters, ROIListData finalRoiInput, ImagePlus finalMaskInput) {
        List<Float> thresholds = new ArrayList<>();
        TFloatArrayList pixels = new TFloatArrayList(inputImage.getWidth() * inputImage.getHeight());
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ImageProcessor mask = getMask(inputImage.getWidth(),
                    inputImage.getHeight(),
                    finalRoiInput,
                    finalMaskInput,
                    index
            );
            pixels.clear();
            getMaskedPixels(ip, mask, pixels);
            ImageStatistics statistics = new FloatProcessor(pixels.size(), 1, pixels.toArray(), inputImage.getProcessor().getColorModel()).getStatistics();
            float threshold = getThreshold(parameters, statistics);
            thresholds.add(threshold);
        }, progressInfo.resolve("Finding thresholds"));

        // Combine thresholds
        ExpressionParameters variableSet = new ExpressionParameters();
        variableSet.set("thresholds", thresholds);
        Number combined = (Number) thresholdCombinationExpression.evaluate(variableSet);
        float threshold = combined.floatValue();
        List<JIPipeAnnotation> annotations = new ArrayList<>();
        if (thresholdAnnotation.isEnabled()) {
            annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
        }
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            // Get target processor and threshold
            ByteProcessor targetProcessor = (ByteProcessor) (outputImage.isStack() ?
                    outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            applyThreshold((FloatProcessor) ip, targetProcessor, threshold);
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(),
                new ImagePlusGreyscaleMaskData(outputImage),
                annotations,
                JIPipeAnnotationMergeStrategy.OverwriteExisting,
                progressInfo);
    }

    private void thresholdCombineStatistics(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo, ImagePlus inputImage, ImagePlus outputImage, ExpressionParameters parameters, ROIListData finalRoiInput, ImagePlus finalMaskInput) {
        TFloatArrayList pixels = new TFloatArrayList(inputImage.getWidth() * inputImage.getHeight() *
                inputImage.getNFrames() * inputImage.getNChannels() * inputImage.getNSlices());
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ImageProcessor mask = getMask(inputImage.getWidth(),
                    inputImage.getHeight(),
                    finalRoiInput,
                    finalMaskInput,
                    index
            );
            getMaskedPixels(ip, mask, pixels);
        }, progressInfo.resolve("Combining pixels"));
        ImageStatistics statistics = new FloatProcessor(pixels.size(), 1, pixels.toArray(), inputImage.getProcessor().getColorModel()).getStatistics();
        float threshold = getThreshold(parameters, statistics);
        List<JIPipeAnnotation> annotations = new ArrayList<>();
        if (thresholdAnnotation.isEnabled()) {
            annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
        }
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            // Get target processor and threshold
            ByteProcessor targetProcessor = (ByteProcessor) (outputImage.isStack() ?
                    outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            applyThreshold((FloatProcessor) ip, targetProcessor, threshold);
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(),
                new ImagePlusGreyscaleMaskData(outputImage),
                annotations,
                JIPipeAnnotationMergeStrategy.Merge,
                progressInfo);
    }

    private void thresholdApplyPerSlice(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo, ImagePlus inputImage, ImagePlus outputImage, ExpressionParameters parameters, ROIListData finalRoiInput, ImagePlus finalMaskInput) {
        List<Float> thresholds = new ArrayList<>();
        TFloatArrayList pixels = new TFloatArrayList(inputImage.getWidth() * inputImage.getHeight());
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ImageProcessor mask = getMask(inputImage.getWidth(),
                    inputImage.getHeight(),
                    finalRoiInput,
                    finalMaskInput,
                    index
            );

            // Collect all pixels
            pixels.clear();
            getMaskedPixels(ip, mask, pixels);
            ImageStatistics statistics = new FloatProcessor(pixels.size(), 1, pixels.toArray(), inputImage.getProcessor().getColorModel()).getStatistics();
            // Get target processor and threshold
            ByteProcessor targetProcessor = (ByteProcessor) (outputImage.isStack() ?
                    outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            float threshold = getThreshold(parameters, statistics);
            applyThreshold((FloatProcessor) ip, targetProcessor, threshold);
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
                new ImagePlusGreyscaleMaskData(outputImage),
                annotations,
                thresholdAnnotationStrategy,
                progressInfo);
    }

    private void getMaskedPixels(ImageProcessor ip, ImageProcessor mask, TFloatArrayList target) {
        float[] imageBytes = (float[]) ip.getPixels();
        byte[] maskBytes = mask != null ? (byte[]) mask.getPixels() : null;
        for (int i = 0; i < imageBytes.length; i++) {
            if(mask == null || Byte.toUnsignedInt(maskBytes[i]) > 0) {
                target.add(imageBytes[i]);
            }
        }
    }

    private float getThreshold(ExpressionParameters parameters, ImageStatistics statistics) {
        parameters.set("histogram", Longs.asList(statistics.getHistogram()));
        parameters.set("num_pixels", statistics.area);
        parameters.set("min_value", statistics.min);
        parameters.set("max_value", statistics.max);
        parameters.set("mean_value", statistics.mean);
        parameters.set("stdev_value", statistics.stdDev);
        parameters.set("modal_value", statistics.mode);
        Number number = (Number) thresholdCalculationExpression.evaluate(parameters);
        return number.floatValue();
    }

    private void applyThreshold(FloatProcessor source, ByteProcessor target, float threshold) {
        float[] src = (float[]) source.getPixels();
        byte[] dst = (byte[]) target.getPixels();
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i] > threshold ? (byte)255 : 0;
        }
    }

    @JIPipeDocumentation(name = "Thresholding function", description = "This expression is executed for each set of slices. " +
            "The expression should return a number that will be used as threshold. A pixel is set to 255 if its value is larger than " +
            "this threshold.")
    @JIPipeParameter("thresholding-function")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public DefaultExpressionParameter getThresholdCalculationExpression() {
        return thresholdCalculationExpression;
    }

    @JIPipeParameter("thresholding-function")
    public void setThresholdCalculationExpression(DefaultExpressionParameter thresholdCalculationExpression) {
        this.thresholdCalculationExpression = thresholdCalculationExpression;
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

    @JIPipeDocumentation(name = "Threshold annotation strategy", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeAnnotationMergeStrategy getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeAnnotationMergeStrategy thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }

    @JIPipeDocumentation(name = "Multi-slice thresholding", description = "Determines how thresholds are calculated if an image has multiple slices. " +
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

    private ImageProcessor getMask(int width, int height, ROIListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
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

    public static class VariableSource implements ExpressionParameterVariableSource {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            result.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
            result.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
            result.add(new ExpressionParameterVariable("Image Z slices", "The number of Z slices in the image", "size_z"));
            result.add(new ExpressionParameterVariable("Image channels", "The number of channel (C) slices in the image", "size_c"));
            result.add(new ExpressionParameterVariable("Image frames", "The number of frames (T) in the image", "size_t"));
            result.add(new ExpressionParameterVariable("Histogram",
                    "An array the represents the histogram (index is the pixel value, value is the number of pixels with this value) of the currently analyzed area",
                    "histogram"));
            result.add(new ExpressionParameterVariable("Number of pixels",
                    "The number of pixels that are incorporated in the statistics",
                    "num_pixels"));
            result.add(new ExpressionParameterVariable("Minimum pixel value",
                    "The minimum pixel value",
                    "min_value"));
            result.add(new ExpressionParameterVariable("Maximum pixel value",
                    "The maximum pixel value",
                    "max_value"));
            result.add(new ExpressionParameterVariable("Mean pixel value",
                    "The mean pixel value",
                    "mean_value"));
            result.add(new ExpressionParameterVariable("Standard deviation pixel value",
                    "The standard deviation of the pixel values",
                    "stdev_value"));
            result.add(new ExpressionParameterVariable("Modal pixel value",
                    "The modes of the pixel values",
                    "modal_value"));
            return result;
        }
    }
}
