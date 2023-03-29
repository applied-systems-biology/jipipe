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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

@JIPipeDocumentation(name = "Adjust displayed contrast", description = "Re-calibrates the incoming image, so its color range is displayed differently by ImageJ. " +
        "This does not change the pixel data.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Contrast")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust", aliasName = "Brightness/Contrast...")
public class DisplayRangeCalibrationAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private ImageJCalibrationMode calibrationMode = ImageJCalibrationMode.AutomaticImageJ;
    private double customMin = 0;
    private double customMax = 1;

    public DisplayRangeCalibrationAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DisplayRangeCalibrationAlgorithm(DisplayRangeCalibrationAlgorithm other) {
        super(other);
        this.calibrationMode = other.calibrationMode;
        this.customMin = other.customMin;
        this.customMax = other.customMax;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        ImageJUtils.calibrate(image, calibrationMode, customMin, customMax);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
    }

    @JIPipeDocumentation(name = "Calibration method", description = "The method to apply for calibration.")
    @JIPipeParameter("calibration-mode")
    public ImageJCalibrationMode getCalibrationMode() {
        return calibrationMode;
    }

    @JIPipeParameter("calibration-mode")
    public void setCalibrationMode(ImageJCalibrationMode calibrationMode) {
        this.calibrationMode = calibrationMode;
    }

    @JIPipeDocumentation(name = "Custom min", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom minimum value.")
    @JIPipeParameter("custom-min")
    public double getCustomMin() {
        return customMin;
    }

    @JIPipeParameter("custom-min")
    public void setCustomMin(double customMin) {
        this.customMin = customMin;
    }

    @JIPipeDocumentation(name = "Custom max", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom maximum value.")
    @JIPipeParameter("custom-max")
    public double getCustomMax() {
        return customMax;
    }

    @JIPipeParameter("custom-max")
    public void setCustomMax(double customMax) {
        this.customMax = customMax;
    }
}
