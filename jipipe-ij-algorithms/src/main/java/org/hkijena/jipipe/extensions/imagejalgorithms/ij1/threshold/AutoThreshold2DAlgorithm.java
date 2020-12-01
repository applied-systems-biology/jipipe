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
import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Segmenter node that thresholds via an auto threshold
 */
@JIPipeDocumentation(name = "Auto threshold 2D", description = "Applies an auto-thresholding algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class AutoThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private AutoThresholder.Method method = AutoThresholder.Method.Default;
    private boolean darkBackground = true;

    /**
     * @param info the info
     */
    public AutoThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
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
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        AutoThresholder autoThresholder = new AutoThresholder();
        ImageJUtils.forEachSlice(img, ip -> {
            if (!darkBackground)
                ip.invert();
            int threshold = autoThresholder.getThreshold(method, ip.getHistogram());
            ip.threshold(threshold);
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
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
}
