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

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.BlackToWhiteTrackBackground;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.ranges.IntNumberRangeParameter;
import org.hkijena.jipipe.extensions.parameters.library.ranges.NumberRangeInvertedMode;
import org.hkijena.jipipe.extensions.parameters.library.ranges.NumberRangeParameterSettings;

import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Manual threshold 2D (8-bit)", description = "Thresholds the image with a manual threshold. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class ManualThreshold8U2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntNumberRangeParameter threshold = new IntNumberRangeParameter(0, 256);
    private OptionalAnnotationNameParameter minThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold", true);
    private OptionalAnnotationNameParameter maxThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold", true);
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ManualThreshold8U2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", "", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ManualThreshold8U2DAlgorithm(ManualThreshold8U2DAlgorithm other) {
        super(other);
        this.threshold = new IntNumberRangeParameter(other.threshold);
        this.minThresholdAnnotation = new OptionalAnnotationNameParameter(other.minThresholdAnnotation);
        this.maxThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxThresholdAnnotation);
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        int minThreshold = threshold.getMin();
        int maxThreshold = threshold.getMax();
        ImageJUtils.forEachSlice(img, ip -> {
            for (int i = 0; i < ip.getPixelCount(); i++) {
                int v = ip.get(i);
                if (v > maxThreshold)
                    ip.set(i, 0);
                else if (v <= minThreshold)
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
        dataBatch.addOutputData(getFirstOutputSlot(),
                new ImagePlusGreyscaleMaskData(img),
                annotations,
                thresholdAnnotationStrategy,
                progressInfo);
    }

    @JIPipeDocumentation(name = "Threshold", description = "Determines the threshold. If min and max are inverted, values outside the defined range are returned")
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = BlackToWhiteTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    @JIPipeParameter("threshold")
    public IntNumberRangeParameter getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(IntNumberRangeParameter threshold) {
        this.threshold = threshold;
    }

    @JIPipeDocumentation(name = "Min threshold annotation", description = "Annotation added to the output that contains the min threshold")
    @JIPipeParameter("min-threshold-annotation")
    public OptionalAnnotationNameParameter getMinThresholdAnnotation() {
        return minThresholdAnnotation;
    }

    @JIPipeParameter("min-threshold-annotation")
    public void setMinThresholdAnnotation(OptionalAnnotationNameParameter minThresholdAnnotation) {
        this.minThresholdAnnotation = minThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Max threshold annotation", description = "Annotation added to the output that contains the max threshold")
    @JIPipeParameter("max-threshold-annotation")
    public OptionalAnnotationNameParameter getMaxThresholdAnnotation() {
        return maxThresholdAnnotation;
    }

    @JIPipeParameter("max-threshold-annotation")
    public void setMaxThresholdAnnotation(OptionalAnnotationNameParameter maxThresholdAnnotation) {
        this.maxThresholdAnnotation = maxThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Threshold annotation strategy", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeTextAnnotationMergeMode getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }
}

