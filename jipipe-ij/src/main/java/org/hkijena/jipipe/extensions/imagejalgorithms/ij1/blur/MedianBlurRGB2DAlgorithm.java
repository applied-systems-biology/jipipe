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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.categories.ProcessorNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Median blur 2D (RGB color)", description = "Applies a 3x3 median filter to RGB color images. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Blur", nodeTypeCategory = ProcessorNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusColorRGBData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class MedianBlurRGB2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MedianBlurRGB2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusColorRGBData.class)
                .addOutputSlot("Output", ImagePlusColorRGBData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public MedianBlurRGB2DAlgorithm(MedianBlurRGB2DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusColorRGBData.class);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachSlice(img, ImageProcessor::medianFilter);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(img));
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }
}
