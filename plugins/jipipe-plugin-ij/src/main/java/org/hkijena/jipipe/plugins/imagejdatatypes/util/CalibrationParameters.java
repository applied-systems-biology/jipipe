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

package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

public class CalibrationParameters extends AbstractJIPipeParameterCollection {
    private ImageJCalibrationMode calibrationMode = ImageJCalibrationMode.AutomaticImageJ;
    private double customMin = 0;
    private double customMax = 1;

    public CalibrationParameters() {
    }

    public CalibrationParameters(CalibrationParameters other) {
        this.calibrationMode = other.calibrationMode;
        this.customMin = other.customMin;
        this.customMax = other.customMax;
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
    @JIPipeParameter("custom-min")
    public double getCustomMin() {
        return customMin;
    }

    @JIPipeParameter("custom-min")
    public void setCustomMin(double customMin) {
        this.customMin = customMin;
    }

    @SetJIPipeDocumentation(name = "Custom max", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom maximum value.")
    @JIPipeParameter("custom-max")
    public double getCustomMax() {
        return customMax;
    }

    @JIPipeParameter("custom-max")
    public void setCustomMax(double customMax) {
        this.customMax = customMax;
    }
}
