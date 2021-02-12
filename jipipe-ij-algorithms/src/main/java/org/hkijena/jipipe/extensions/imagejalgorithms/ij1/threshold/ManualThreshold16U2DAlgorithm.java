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
import ij.process.ShortProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Manual threshold 2D (16-bit)", description = "Thresholds the image with a manual threshold. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale16UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class ManualThreshold16U2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int minThreshold = 0;
    private int maxThreshold = 65535;
    private OptionalAnnotationNameParameter minThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold", true);
    private OptionalAnnotationNameParameter maxThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold", true);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ManualThreshold16U2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale16UData.class)
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
    public ManualThreshold16U2DAlgorithm(ManualThreshold16U2DAlgorithm other) {
        super(other);
        this.minThreshold = other.minThreshold;
        this.maxThreshold = other.maxThreshold;
        this.minThresholdAnnotation = new OptionalAnnotationNameParameter(other.minThresholdAnnotation);
        this.maxThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxThresholdAnnotation);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale16UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachSlice(img, ip -> {
            for (int i = 0; i < ip.getPixelCount(); i++) {
                int v = ip.get(i);
                if (v > maxThreshold)
                    ip.set(i, 0);
                else if (v <= minThreshold)
                    ip.set(i, 0);
                else
                    ip.set(i, 255);
                ((ShortProcessor) ip).findMinAndMax();
            }
        }, progressInfo);
        List<JIPipeAnnotation> annotations = new ArrayList<>();
        if(minThresholdAnnotation.isEnabled()) {
            annotations.add(minThresholdAnnotation.createAnnotation("" + minThreshold));
        }
        if(maxThresholdAnnotation.isEnabled()) {
            annotations.add(maxThresholdAnnotation.createAnnotation("" + maxThreshold));
        }
        dataBatch.addOutputData(getFirstOutputSlot(),
                new ImagePlusGreyscaleMaskData(img),
                annotations,
                JIPipeAnnotationMergeStrategy.Merge,
                progressInfo);
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Min threshold").checkIfWithin(this, minThreshold, 0, 65535, true, true);
        report.forCategory("Max threshold").checkIfWithin(this, maxThreshold, 0, 65535, true, true);
        if (maxThreshold < minThreshold) {
            report.forCategory("Thresholds").reportIsInvalid("Max threshold is smaller than min threshold!", "The maximum pixel value to keep is less than the minimum pixel value to keep.",
                    "Please update the parameters", this);
        }
    }

    @JIPipeDocumentation(name = "Min threshold", description = "All pixel values less or equal to this are set to zero. The value interval is [0, 65535].")
    @JIPipeParameter(value = "min-threshold", uiOrder = -50)
    public int getMinThreshold() {
        return minThreshold;
    }

    @JIPipeParameter("min-threshold")
    public void setMinThreshold(int minThreshold) {
        this.minThreshold = minThreshold;

    }

    @JIPipeDocumentation(name = "Max threshold", description = "All pixel values greater than this are set to zero. The value interval is [0, 65535].")
    @JIPipeParameter(value = "max-threshold", uiOrder = -40)
    public int getMaxThreshold() {
        return maxThreshold;
    }

    @JIPipeParameter("max-threshold")
    public void setMaxThreshold(int maxThreshold) {
        this.maxThreshold = maxThreshold;
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
    @JIPipeParameter("min-threshold-annotation")
    public OptionalAnnotationNameParameter getMaxThresholdAnnotation() {
        return maxThresholdAnnotation;
    }

    @JIPipeParameter("min-threshold-annotation")
    public void setMaxThresholdAnnotation(OptionalAnnotationNameParameter maxThresholdAnnotation) {
        this.maxThresholdAnnotation = maxThresholdAnnotation;
    }
}
