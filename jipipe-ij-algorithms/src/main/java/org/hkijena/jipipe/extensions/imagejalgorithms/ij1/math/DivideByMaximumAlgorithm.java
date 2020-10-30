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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.DisplayRangeCalibrationAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.CalibrationMode;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Divide by maximum", description = "Divides greyscale pixel values by the global maximum across all slices.")
@JIPipeOrganization(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output")
public class DivideByMaximumAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean recalibrate = true;


    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public DivideByMaximumAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale32FData.class)
                .addOutputSlot("Output", ImagePlusGreyscale32FData.class, null)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public DivideByMaximumAlgorithm(DivideByMaximumAlgorithm other) {
        super(other);
        this.recalibrate = other.recalibrate;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getDuplicateImage();
        double[] max = new double[]{Double.NEGATIVE_INFINITY};
        ImageJUtils.forEachSlice(img, ip -> max[0] = Math.max(ip.getStatistics().max, max[0]));
        ImageJUtils.forEachSlice(img, ip -> ip.multiply(1.0 / max[0]));
        if (recalibrate) {
            DisplayRangeCalibrationAlgorithm.calibrate(img, CalibrationMode.AutomaticImageJ, 0, 1);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }

    @JIPipeDocumentation(name = "Recalibrate afterwards", description = "Without this setting enabled, the generated image might only be shown as black in ImageJ.")
    @JIPipeParameter("recalibrate")
    public boolean isRecalibrate() {
        return recalibrate;
    }

    @JIPipeParameter("recalibrate")
    public void setRecalibrate(boolean recalibrate) {
        this.recalibrate = recalibrate;
    }
}
