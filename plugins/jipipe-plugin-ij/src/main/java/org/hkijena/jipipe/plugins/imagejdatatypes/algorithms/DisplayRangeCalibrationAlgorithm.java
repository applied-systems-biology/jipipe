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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

@SetJIPipeDocumentation(name = "Adjust displayed contrast", description = "Re-calibrates the incoming image, so its color range is displayed differently by ImageJ. " +
        "This does not change the pixel data.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Contrast")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust", aliasName = "Brightness/Contrast...")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        ImageJUtils.calibrate(image, calibrationMode, customMin, customMax);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Calibration method", description = "The method to apply for calibration.")
    @JIPipeParameter("calibration-mode")
    public ImageJCalibrationMode getCalibrationMode() {
        return calibrationMode;
    }

    @JIPipeParameter("calibration-mode")
    public void setCalibrationMode(ImageJCalibrationMode calibrationMode) {
        this.calibrationMode = calibrationMode;
    }

    @SetJIPipeDocumentation(name = "Custom min", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom minimum value.")
    @JIPipeParameter(value = "custom-min", uiOrder = -99)
    public double getCustomMin() {
        return customMin;
    }

    @JIPipeParameter("custom-min")
    public void setCustomMin(double customMin) {
        this.customMin = customMin;
    }

    @SetJIPipeDocumentation(name = "Custom max", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom maximum value.")
    @JIPipeParameter(value = "custom-max", uiOrder = -80)
    public double getCustomMax() {
        return customMax;
    }

    @JIPipeParameter("custom-max")
    public void setCustomMax(double customMax) {
        this.customMax = customMax;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (calibrationMode == ImageJCalibrationMode.Custom && customMax < customMin) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Invalid custom min/max",
                    "The custom min cannot be larger than the custom max"));
        }
    }
}
