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

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Threshold by annotations", description = "Thresholds the image with a manual threshold provided by annotations. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class ThresholdByAnnotation2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter minThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold", true);
    private OptionalAnnotationNameParameter maxThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold", false);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ThresholdByAnnotation2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscaleData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ThresholdByAnnotation2DAlgorithm(ThresholdByAnnotation2DAlgorithm other) {
        super(other);
        this.minThresholdAnnotation = new OptionalAnnotationNameParameter(other.minThresholdAnnotation);
        this.maxThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxThresholdAnnotation);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo);
        ImagePlus inputImage = inputData.getDuplicateImage();
        ImagePlus outputImage = IJ.createHyperStack(inputImage.getTitle() + " Thresholded",
                inputImage.getWidth(),
                inputImage.getHeight(),
                inputImage.getNChannels(),
                inputImage.getNSlices(),
                inputImage.getNFrames(),
                8);

        float minThreshold = Float.NEGATIVE_INFINITY;
        float maxThreshold = Float.POSITIVE_INFINITY;

        if (minThresholdAnnotation.isEnabled()) {
            JIPipeAnnotation annotation = dataBatch.getMergedAnnotation(minThresholdAnnotation.getContent());
            if (annotation != null) {
                minThreshold = NumberUtils.createFloat(annotation.getValue().replace(',', '.'));
            }
        }
        if (maxThresholdAnnotation.isEnabled()) {
            JIPipeAnnotation annotation = dataBatch.getMergedAnnotation(maxThresholdAnnotation.getContent());
            if (annotation != null) {
                maxThreshold = NumberUtils.createFloat(annotation.getValue().replace(',', '.'));
            }
        }

        float finalMaxThreshold = maxThreshold;
        float finalMinThreshold = minThreshold;
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ByteProcessor targetProcessor = (ByteProcessor) (outputImage.isStack() ?
                    outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            for (int i = 0; i < ip.getPixelCount(); i++) {
                float v = ip.getf(i);
                if (v > finalMaxThreshold)
                    targetProcessor.set(i, 0);
                else if (v <= finalMinThreshold)
                    targetProcessor.set(i, 0);
                else
                    targetProcessor.set(i, 255);
            }
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(),
                new ImagePlusGreyscaleMaskData(outputImage),
                progressInfo);
    }

    @JIPipeDocumentation(name = "Min threshold annotation", description = "Annotation that contains the minimum pixel value. If disabled, this assumes the negative infinity.")
    @JIPipeParameter("min-threshold-annotation")
    public OptionalAnnotationNameParameter getMinThresholdAnnotation() {
        return minThresholdAnnotation;
    }

    @JIPipeParameter("min-threshold-annotation")
    public void setMinThresholdAnnotation(OptionalAnnotationNameParameter minThresholdAnnotation) {
        this.minThresholdAnnotation = minThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Max threshold annotation", description = "Annotation that contains the maximum pixel value. If disabled, this assumes positive infinity.")
    @JIPipeParameter("max-threshold-annotation")
    public OptionalAnnotationNameParameter getMaxThresholdAnnotation() {
        return maxThresholdAnnotation;
    }

    @JIPipeParameter("max-threshold-annotation")
    public void setMaxThresholdAnnotation(OptionalAnnotationNameParameter maxThresholdAnnotation) {
        this.maxThresholdAnnotation = maxThresholdAnnotation;
    }
}
