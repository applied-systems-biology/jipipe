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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.threshold;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;


/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Threshold by annotations", description = "Thresholds the image with a manual threshold provided by annotations. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust")
public class ThresholdByAnnotation2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter minThresholdAnnotation = new OptionalTextAnnotationNameParameter("Min Threshold", true);
    private OptionalTextAnnotationNameParameter maxThresholdAnnotation = new OptionalTextAnnotationNameParameter("Max Threshold", false);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ThresholdByAnnotation2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ThresholdByAnnotation2DAlgorithm(ThresholdByAnnotation2DAlgorithm other) {
        super(other);
        this.minThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.minThresholdAnnotation);
        this.maxThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.maxThresholdAnnotation);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo);
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
            JIPipeTextAnnotation annotation = iterationStep.getMergedTextAnnotation(minThresholdAnnotation.getContent());
            if (annotation != null) {
                minThreshold = NumberUtils.createFloat(annotation.getValue().replace(',', '.'));
            }
        }
        if (maxThresholdAnnotation.isEnabled()) {
            JIPipeTextAnnotation annotation = iterationStep.getMergedTextAnnotation(maxThresholdAnnotation.getContent());
            if (annotation != null) {
                maxThreshold = NumberUtils.createFloat(annotation.getValue().replace(',', '.'));
            }
        }

        float finalMaxThreshold = maxThreshold;
        float finalMinThreshold = minThreshold;
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ByteProcessor targetProcessor = (ByteProcessor) (outputImage.hasImageStack() ?
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
        iterationStep.addOutputData(getFirstOutputSlot(),
                new ImagePlusGreyscaleMaskData(outputImage),
                progressInfo);
    }

    @SetJIPipeDocumentation(name = "Min threshold annotation", description = "Annotation that contains the minimum pixel value. If disabled, this assumes the negative infinity.")
    @JIPipeParameter("min-threshold-annotation")
    public OptionalTextAnnotationNameParameter getMinThresholdAnnotation() {
        return minThresholdAnnotation;
    }

    @JIPipeParameter("min-threshold-annotation")
    public void setMinThresholdAnnotation(OptionalTextAnnotationNameParameter minThresholdAnnotation) {
        this.minThresholdAnnotation = minThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Max threshold annotation", description = "Annotation that contains the maximum pixel value. If disabled, this assumes positive infinity.")
    @JIPipeParameter("max-threshold-annotation")
    public OptionalTextAnnotationNameParameter getMaxThresholdAnnotation() {
        return maxThresholdAnnotation;
    }

    @JIPipeParameter("max-threshold-annotation")
    public void setMaxThresholdAnnotation(OptionalTextAnnotationNameParameter maxThresholdAnnotation) {
        this.maxThresholdAnnotation = maxThresholdAnnotation;
    }
}
