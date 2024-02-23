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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Manual threshold 2D (32-bit)", description = "Thresholds the image with a manual threshold. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust")
public class ManualThreshold32F2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private float minThreshold = 0;
    private float maxThreshold = Float.POSITIVE_INFINITY;
    private OptionalTextAnnotationNameParameter minThresholdAnnotation = new OptionalTextAnnotationNameParameter("Min Threshold", true);
    private OptionalTextAnnotationNameParameter maxThresholdAnnotation = new OptionalTextAnnotationNameParameter("Max Threshold", true);
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ManualThreshold32F2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ManualThreshold32F2DAlgorithm(ManualThreshold32F2DAlgorithm other) {
        super(other);
        this.minThreshold = other.minThreshold;
        this.maxThreshold = other.maxThreshold;
        this.minThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.minThresholdAnnotation);
        this.maxThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.maxThresholdAnnotation);
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
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
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ByteProcessor targetProcessor = (ByteProcessor) (outputImage.hasImageStack() ?
                    outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            for (int i = 0; i < ip.getPixelCount(); i++) {
                float v = ip.getf(i);
                if (v > maxThreshold)
                    targetProcessor.set(i, 0);
                else if (v < minThreshold)
                    targetProcessor.set(i, 0);
                else
                    targetProcessor.set(i, 255);
            }
        }, progressInfo);
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        if (minThresholdAnnotation.isEnabled()) {
            annotations.add(minThresholdAnnotation.createAnnotation("" + minThreshold));
        }
        if (maxThresholdAnnotation.isEnabled()) {
            annotations.add(maxThresholdAnnotation.createAnnotation("" + maxThreshold));
        }
        iterationStep.addOutputData(getFirstOutputSlot(),
                new ImagePlusGreyscaleMaskData(outputImage),
                annotations,
                thresholdAnnotationStrategy,
                progressInfo);
    }

    @SetJIPipeDocumentation(name = "Min threshold", description = "All pixel values less or equal to this are set to zero.")
    @JIPipeParameter(value = "min-threshold", uiOrder = -50)
    public float getMinThreshold() {
        return minThreshold;
    }

    @JIPipeParameter("min-threshold")
    public void setMinThreshold(float minThreshold) {
        this.minThreshold = minThreshold;

    }

    @SetJIPipeDocumentation(name = "Max threshold", description = "All pixel values greater than this are set to zero.")
    @JIPipeParameter(value = "max-threshold", uiOrder = -40)
    public float getMaxThreshold() {
        return maxThreshold;
    }

    @JIPipeParameter("max-threshold")
    public void setMaxThreshold(float maxThreshold) {
        this.maxThreshold = maxThreshold;
    }

    @SetJIPipeDocumentation(name = "Min threshold annotation", description = "Annotation added to the output that contains the min threshold")
    @JIPipeParameter("min-threshold-annotation")
    public OptionalTextAnnotationNameParameter getMinThresholdAnnotation() {
        return minThresholdAnnotation;
    }

    @JIPipeParameter("min-threshold-annotation")
    public void setMinThresholdAnnotation(OptionalTextAnnotationNameParameter minThresholdAnnotation) {
        this.minThresholdAnnotation = minThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Max threshold annotation", description = "Annotation added to the output that contains the max threshold")
    @JIPipeParameter("max-threshold-annotation")
    public OptionalTextAnnotationNameParameter getMaxThresholdAnnotation() {
        return maxThresholdAnnotation;
    }

    @JIPipeParameter("max-threshold-annotation")
    public void setMaxThresholdAnnotation(OptionalTextAnnotationNameParameter maxThresholdAnnotation) {
        this.maxThresholdAnnotation = maxThresholdAnnotation;
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
}
