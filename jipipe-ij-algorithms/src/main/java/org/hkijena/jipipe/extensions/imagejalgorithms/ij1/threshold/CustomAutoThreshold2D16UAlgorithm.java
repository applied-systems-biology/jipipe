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
import gnu.trove.list.array.TShortArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
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
@JIPipeDocumentation(name = "Custom auto threshold 2D (16-bit)", description = "Allows to implement a custom thresholding method via expressions. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale16UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class CustomAutoThreshold2D16UAlgorithm extends JIPipeIteratingAlgorithm {

    private DefaultExpressionParameter thresholdCalculationExpression = new DefaultExpressionParameter("(stat_max + stat_min) / 2");
    private OptionalAnnotationNameParameter thresholdAnnotation = new OptionalAnnotationNameParameter("Threshold", true);
    private AutoThreshold2DAlgorithm.SliceThresholdMode thresholdMode = AutoThreshold2DAlgorithm.SliceThresholdMode.ApplyPerSlice;

    private DefaultExpressionParameter thresholdCombinationExpression = new DefaultExpressionParameter("MIN(thresholds)");
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private JIPipeAnnotationMergeStrategy thresholdAnnotationStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

    private boolean accessPixels = true;

    /**
     * @param info the info
     */
    public CustomAutoThreshold2D16UAlgorithm(JIPipeNodeInfo info) {
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
    public CustomAutoThreshold2D16UAlgorithm(CustomAutoThreshold2D16UAlgorithm other) {
        super(other);
        this.thresholdCalculationExpression = new DefaultExpressionParameter(other.thresholdCalculationExpression);
        this.thresholdAnnotation = new OptionalAnnotationNameParameter(other.thresholdAnnotation);
        this.thresholdMode = other.thresholdMode;
        this.thresholdCombinationExpression = new DefaultExpressionParameter(other.thresholdCombinationExpression);
        this.sourceArea = other.sourceArea;
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
        this.accessPixels = other.accessPixels;
        updateRoiSlot();
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale16UData.class, progressInfo);
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

        for (JIPipeAnnotation annotation : dataBatch.getAnnotations().values()) {
            parameters.set(annotation.getName(), annotation.getValue());
        }

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
        List<Integer> thresholds = new ArrayList<>();
        TShortArrayList pixels = new TShortArrayList(inputImage.getWidth() * inputImage.getHeight());
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ImageProcessor mask = getMask(inputImage.getWidth(),
                    inputImage.getHeight(),
                    finalRoiInput,
                    finalMaskInput,
                    index
            );
            pixels.clear();
            getMaskedPixels(ip, mask, pixels);
            ImageStatistics statistics = new ShortProcessor(pixels.size(), 1, pixels.toArray(), inputImage.getProcessor().getColorModel()).getStatistics();
            int threshold = getThreshold(parameters, statistics, pixels);
            thresholds.add(threshold);
        }, progressInfo.resolve("Finding thresholds"));

        // Combine thresholds
        ExpressionParameters variableSet = new ExpressionParameters();
        variableSet.set("thresholds", thresholds);
        Number combined = (Number) thresholdCombinationExpression.evaluate(variableSet);
        int threshold = combined.intValue();
        List<JIPipeAnnotation> annotations = new ArrayList<>();
        if (thresholdAnnotation.isEnabled()) {
            annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
        }
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            // Get target processor and threshold
            ByteProcessor targetProcessor = (ByteProcessor) (outputImage.isStack() ?
                    outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            applyThreshold((ShortProcessor) ip, targetProcessor, threshold);
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(),
                new ImagePlusGreyscaleMaskData(outputImage),
                annotations,
                JIPipeAnnotationMergeStrategy.OverwriteExisting,
                progressInfo);
    }

    private void thresholdCombineStatistics(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo, ImagePlus inputImage, ImagePlus outputImage, ExpressionParameters parameters, ROIListData finalRoiInput, ImagePlus finalMaskInput) {
        TShortArrayList pixels = new TShortArrayList(inputImage.getWidth() * inputImage.getHeight() *
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
        ImageStatistics statistics = new ShortProcessor(pixels.size(), 1, pixels.toArray(), inputImage.getProcessor().getColorModel()).getStatistics();
        int threshold = getThreshold(parameters, statistics, pixels);
        List<JIPipeAnnotation> annotations = new ArrayList<>();
        if (thresholdAnnotation.isEnabled()) {
            annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
        }
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            // Get target processor and threshold
            ByteProcessor targetProcessor = (ByteProcessor) (outputImage.isStack() ?
                    outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            applyThreshold((ShortProcessor) ip, targetProcessor, threshold);
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(),
                new ImagePlusGreyscaleMaskData(outputImage),
                annotations,
                JIPipeAnnotationMergeStrategy.Merge,
                progressInfo);
    }

    private void thresholdApplyPerSlice(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo, ImagePlus inputImage, ImagePlus outputImage, ExpressionParameters parameters, ROIListData finalRoiInput, ImagePlus finalMaskInput) {
        List<Integer> thresholds = new ArrayList<>();
        TShortArrayList pixels = new TShortArrayList(inputImage.getWidth() * inputImage.getHeight());
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
            ImageStatistics statistics = new ShortProcessor(pixels.size(), 1, pixels.toArray(), inputImage.getProcessor().getColorModel()).getStatistics();
            int threshold = getThreshold(parameters, statistics, pixels);

            // Get target processor and threshold
            ByteProcessor targetProcessor = (ByteProcessor) (outputImage.isStack() ?
                                    outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                                    : outputImage.getProcessor());
            applyThreshold((ShortProcessor) ip, targetProcessor, threshold);

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

    private void getMaskedPixels(ImageProcessor ip, ImageProcessor mask, TShortArrayList target) {
        short[] imageBytes = (short[]) ip.getPixels();
        byte[] maskBytes = mask != null ? (byte[]) mask.getPixels() : null;
        for (int i = 0; i < imageBytes.length; i++) {
            if(mask == null || Byte.toUnsignedInt(maskBytes[i]) > 0) {
                target.add(imageBytes[i]);
            }
        }
    }

    private int getThreshold(ExpressionParameters parameters, ImageStatistics statistics, TShortArrayList pixels) {
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
        if(accessPixels) {
            List<Integer> pixelList = new ArrayList<>();
            for (int i = 0; i < pixels.size(); i++) {
                pixelList.add(Short.toUnsignedInt(pixels.get(i)));
            }
            parameters.set("pixels", pixelList);
        }
        Number number = (Number) thresholdCalculationExpression.evaluate(parameters);
        return number.intValue();
    }

    private void applyThreshold(ShortProcessor source, ByteProcessor target, int threshold) {
        short[] src = (short[]) source.getPixels();
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

    @JIPipeDocumentation(name = "Pixels are available as variables", description = "If enabled, the list of all pixels that are considered for the statistics are available as variable 'pixels'. " +
            "Please note that this will slow down the performance due to necessary conversions.")
    @JIPipeParameter("access-pixels")
    public boolean isAccessPixels() {
        return accessPixels;
    }

    @JIPipeParameter("access-pixels")
    public void setAccessPixels(boolean accessPixels) {
        this.accessPixels = accessPixels;
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
            result.add(new ExpressionParameterVariable("<Annotations>",
                    "Annotations of the input image are available (use Update Cache to find the list of annotations)",
                    ""));
            result.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
            result.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
            result.add(new ExpressionParameterVariable("Image Z slices", "The number of Z slices in the image", "size_z"));
            result.add(new ExpressionParameterVariable("Image channels", "The number of channel (C) slices in the image", "size_c"));
            result.add(new ExpressionParameterVariable("Image frames", "The number of frames (T) in the image", "size_t"));
            result.add(new ExpressionParameterVariable("Histogram",
                    "An array the represents the histogram (index is the pixel value, value is the number of pixels with this value) of the currently analyzed area",
                    "stat_histogram"));
            result.add(new ExpressionParameterVariable("Area", "Area of selection in square pixels.", "stat_area"));
            result.add(new ExpressionParameterVariable("Pixel value standard deviation", "Measures the standard deviation of greyscale pixel values", "stat_stdev"));
            result.add(new ExpressionParameterVariable("Pixel value min", "Measures the minimum of greyscale pixel values", "stat_min"));
            result.add(new ExpressionParameterVariable("Pixel value max", "Measures the maximum of greyscale pixel values", "stat_max"));
            result.add(new ExpressionParameterVariable("Pixel value mean", "Measures the mean of greyscale pixel values", "stat_mean"));
            result.add(new ExpressionParameterVariable("Pixel value mode", "Most frequently occurring gray value within the selection", "stat_mode"));
            result.add(new ExpressionParameterVariable("Pixel value median", "The median value of the pixels in the image or selection", "stat_median"));
            result.add(new ExpressionParameterVariable("Pixel value kurtosis", "The fourth order moment about the greyscale pixel value mean", "stat_kurtosis"));
            result.add(new ExpressionParameterVariable("Pixel value integrated density", "The product of Area and Mean Gray Value", "stat_int_den"));
            result.add(new ExpressionParameterVariable("Pixel value raw integrated density", "The sum of the values of the pixels in the image or selection", "stat_raw_int_den"));
            result.add(new ExpressionParameterVariable("Pixel value skewness", "The sum of the values of the pixels in the image or selection", "stat_skewness"));
            result.add(new ExpressionParameterVariable("Area fraction", "The percentage of non-zero pixels", "stat_area_fraction"));
            return result;
        }
    }
}
