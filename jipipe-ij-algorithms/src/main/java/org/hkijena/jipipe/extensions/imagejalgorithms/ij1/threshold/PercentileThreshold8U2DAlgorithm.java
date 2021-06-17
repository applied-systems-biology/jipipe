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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Percentile threshold 2D (8-bit)", description = "Thresholds the image with a threshold calculated from the image pixel values. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class PercentileThreshold8U2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double percentile = 50;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public PercentileThreshold8U2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
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
    public PercentileThreshold8U2DAlgorithm(PercentileThreshold8U2DAlgorithm other) {
        super(other);
        this.percentile = other.percentile;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachSlice(img, ip -> {
            int[] histogram = ip.getHistogram(256);
            double sum = 0;
            for (int i = 0; i < histogram.length; i++) {
                sum += histogram[i];
            }
            double percentileThresh = sum * percentile / 100.0;
            int thresh = 0;
            sum = 0;
            for (int i = 0; i < histogram.length; i++) {
                sum += histogram[i];
                if (sum >= percentileThresh) {
                    thresh = i;
                    break;
                }
            }
            ip.threshold(thresh);
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Percentile").checkIfWithin(this, percentile, 0, 100, true, true);
    }

    @JIPipeDocumentation(name = "Percentile", description = "Percentile from 0-100.")
    @JIPipeParameter("percentile")
    public double getPercentile() {
        return percentile;
    }

    @JIPipeParameter("percentile")
    public void setPercentile(double percentile) {
        this.percentile = percentile;

    }
}
