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
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.BlackToWhiteTrackBackground;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.ranges.IntNumberRangeParameter;
import org.hkijena.jipipe.plugins.parameters.library.ranges.NumberRangeInvertedMode;
import org.hkijena.jipipe.plugins.parameters.library.ranges.NumberRangeParameterSettings;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Manual threshold 2D (8-bit)", description = "Thresholds the image with a manual threshold. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust")
public class ManualThreshold8U2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntNumberRangeParameter threshold = new IntNumberRangeParameter(0, 256);
    private OptionalTextAnnotationNameParameter minThresholdAnnotation = new OptionalTextAnnotationNameParameter("Min Threshold", true);
    private OptionalTextAnnotationNameParameter maxThresholdAnnotation = new OptionalTextAnnotationNameParameter("Max Threshold", true);
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ManualThreshold8U2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ManualThreshold8U2DAlgorithm(ManualThreshold8U2DAlgorithm other) {
        super(other);
        this.threshold = new IntNumberRangeParameter(other.threshold);
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
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        int minThreshold = threshold.getMin();
        int maxThreshold = threshold.getMax();
        ImageJUtils.forEachSlice(img, ip -> {
            for (int i = 0; i < ip.getPixelCount(); i++) {
                int v = ip.get(i);
                if (v > maxThreshold)
                    ip.set(i, 0);
                else if (v < minThreshold)
                    ip.set(i, 0);
                else
                    ip.set(i, 255);
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
                new ImagePlusGreyscaleMaskData(img),
                annotations,
                thresholdAnnotationStrategy,
                progressInfo);
    }

    @SetJIPipeDocumentation(name = "Threshold", description = "Determines the threshold. If min and max are inverted, values outside the defined range are returned")
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = BlackToWhiteTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    @JIPipeParameter("threshold")
    public IntNumberRangeParameter getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(IntNumberRangeParameter threshold) {
        this.threshold = threshold;
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


